package com.fiap.techchallenge.mensageria.usecase;

import com.fiap.techchallenge.mensageria.domain.TipoEvento;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.domain.StatusOrcamento;
import com.fiap.techchallenge.orcamento.inputdata.ItemDiagnosticoInputData;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoGateway;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoUseCase;
import com.fiap.techchallenge.pagamento.domain.Pagamento;
import com.fiap.techchallenge.pagamento.domain.StatusPagamento;
import com.fiap.techchallenge.pagamento.usecase.PagamentoGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class EventoEntradaInteractor implements EventoEntradaUseCase {

    private final OrcamentoUseCase orcamentoUseCase;
    private final OrcamentoGateway orcamentoGateway;
    private final PagamentoGateway pagamentoGateway;
    private final ProcessedEventGateway processedEventGateway;
    private final EventoSaidaGateway eventoSaidaGateway;
    private final Clock clock;

    public EventoEntradaInteractor(OrcamentoUseCase orcamentoUseCase,
                                   OrcamentoGateway orcamentoGateway,
                                   PagamentoGateway pagamentoGateway,
                                   ProcessedEventGateway processedEventGateway,
                                   EventoSaidaGateway eventoSaidaGateway,
                                   Clock clock) {
        this.orcamentoUseCase = orcamentoUseCase;
        this.orcamentoGateway = orcamentoGateway;
        this.pagamentoGateway = pagamentoGateway;
        this.processedEventGateway = processedEventGateway;
        this.eventoSaidaGateway = eventoSaidaGateway;
        this.clock = clock;
    }

    @Override
    public void processarDiagnosticoConcluido(UUID eventId, Long osId,
                                            List<ItemDiagnosticoInputData> pecas,
                                            List<ItemDiagnosticoInputData> servicos) {
        if (processedEventGateway.jaProcessado(eventId)) {
            log.info("[SAGA] Evento {} já processado anteriormente; ignorando duplicidade.", eventId);
            return;
        }

        log.info("[SAGA] Processando DIAGNOSTICO_CONCLUIDO para OS {} com {} peças e {} serviços",
                osId, pecas.size(), servicos.size());

        Orcamento orcamento = this.orcamentoUseCase.gerarOrcamento(osId, pecas, servicos);

        Map<String, Object> payload = Map.of(
                "osId", osId,
                "orcamentoId", orcamento.getId() != null ? orcamento.getId() : "",
                "valorTotal", orcamento.getValorTotal()
        );
        this.eventoSaidaGateway.publicarEvento(TipoEvento.ORCAMENTO_GERADO, String.valueOf(osId), payload);

        this.processedEventGateway.registrarProcessado(eventId, TipoEvento.DIAGNOSTICO_CONCLUIDO, String.valueOf(osId));
    }

    @Override
    public void processarExecucaoFalhou(UUID eventId, Long osId, String motivo, String detalhe) {
        if (processedEventGateway.jaProcessado(eventId)) {
            log.info("[SAGA] Evento {} já processado anteriormente; ignorando duplicidade.", eventId);
            return;
        }

        log.warn("[SAGA] Processando compensação para EXECUCAO_FALHOU na OS {}. Motivo: {} - {}", osId, motivo, detalhe);
        LocalDateTime agora = LocalDateTime.now(clock);

        Optional<Pagamento> pagamentoOpt = this.pagamentoGateway.findUltimoByOsId(osId);
        if (pagamentoOpt.isPresent()) {
            Pagamento pagamento = pagamentoOpt.get();
            if (pagamento.getStatus() == StatusPagamento.APROVADO) {
                log.info("[SAGA COMPENSAÇÃO] Estornando pagamento aprovado da OS {}", osId);
                pagamento.estornar(agora);
                this.pagamentoGateway.save(pagamento);

                Map<String, Object> payloadEstorno = Map.of(
                        "osId", osId,
                        "pagamentoId", pagamento.getIdTransacaoProvedor() != null ? pagamento.getIdTransacaoProvedor() : ""
                );
                this.eventoSaidaGateway.publicarEvento(TipoEvento.PAGAMENTO_ESTORNADO, String.valueOf(osId), payloadEstorno);
            }
        }

        this.orcamentoGateway.findByOsId(osId).ifPresent(orcamento -> {
            if (orcamento.getStatus() == StatusOrcamento.GERADO) {
                orcamento.rejeitar(agora);
                this.orcamentoGateway.save(orcamento);
            }
        });

        this.processedEventGateway.registrarProcessado(eventId, TipoEvento.EXECUCAO_FALHOU, String.valueOf(osId));
    }

    @Override
    public void processarOsCancelada(UUID eventId, Long osId, String motivo) {
        if (processedEventGateway.jaProcessado(eventId)) {
            log.info("[SAGA] Evento {} já processado anteriormente; ignorando duplicidade.", eventId);
            return;
        }

        log.info("[SAGA] Processando OS_CANCELADA para OS {}. Motivo: {}", osId, motivo);
        LocalDateTime agora = LocalDateTime.now(clock);

        this.orcamentoGateway.findByOsId(osId).ifPresent(orcamento -> {
            if (orcamento.getStatus() == StatusOrcamento.GERADO) {
                orcamento.rejeitar(agora);
                this.orcamentoGateway.save(orcamento);
            }
        });

        this.pagamentoGateway.findUltimoByOsId(osId).ifPresent(pagamento -> {
            if (pagamento.getStatus() == StatusPagamento.PENDENTE || pagamento.getStatus() == StatusPagamento.PROCESSANDO) {
                pagamento.cancelar(agora);
                this.pagamentoGateway.save(pagamento);
            }
        });

        this.processedEventGateway.registrarProcessado(eventId, TipoEvento.OS_CANCELADA, String.valueOf(osId));
    }
}
