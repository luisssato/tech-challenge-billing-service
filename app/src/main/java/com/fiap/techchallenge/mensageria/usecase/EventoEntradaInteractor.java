package com.fiap.techchallenge.mensageria.usecase;

import com.fiap.techchallenge.mensageria.domain.TipoEvento;
import com.fiap.techchallenge.observabilidade.BillingEventLogger;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.domain.StatusOrcamento;
import com.fiap.techchallenge.orcamento.inputdata.ItemDiagnosticoInputData;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoGateway;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoUseCase;
import com.fiap.techchallenge.pagamento.domain.Pagamento;
import com.fiap.techchallenge.pagamento.domain.StatusPagamento;
import com.fiap.techchallenge.pagamento.usecase.PagamentoGateway;
import com.fiap.techchallenge.pagamento.usecase.PagamentoUseCase;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventoEntradaInteractor implements EventoEntradaUseCase {

    private final OrcamentoUseCase orcamentoUseCase;
    private final OrcamentoGateway orcamentoGateway;
    private final PagamentoUseCase pagamentoUseCase;
    private final PagamentoGateway pagamentoGateway;
    private final ProcessedEventGateway processedEventGateway;
    private final EventoSaidaGateway eventoSaidaGateway;
    private final Clock clock;

    public EventoEntradaInteractor(OrcamentoUseCase orcamentoUseCase,
                                   OrcamentoGateway orcamentoGateway,
                                   PagamentoUseCase pagamentoUseCase,
                                   PagamentoGateway pagamentoGateway,
                                   ProcessedEventGateway processedEventGateway,
                                   EventoSaidaGateway eventoSaidaGateway,
                                   Clock clock) {
        this.orcamentoUseCase = orcamentoUseCase;
        this.orcamentoGateway = orcamentoGateway;
        this.pagamentoUseCase = pagamentoUseCase;
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
            BillingEventLogger.eventoDuplicado(osId, eventId, TipoEvento.DIAGNOSTICO_CONCLUIDO);
            return;
        }

        Orcamento orcamento = this.orcamentoUseCase.gerarOrcamento(osId, pecas, servicos);

        Map<String, Object> payload = Map.of(
                "osId", osId,
                "orcamentoId", orcamento.getId() != null ? orcamento.getId() : "",
                "valorTotal", orcamento.getValorTotal()
        );
        this.eventoSaidaGateway.publicarEvento(TipoEvento.ORCAMENTO_GERADO, String.valueOf(osId), payload);

        this.processedEventGateway.registrarProcessado(eventId, TipoEvento.DIAGNOSTICO_CONCLUIDO, String.valueOf(osId));
        BillingEventLogger.eventoProcessado(osId, eventId, TipoEvento.DIAGNOSTICO_CONCLUIDO,
                "processar_diagnostico", "sucesso",
                String.format("Diagnóstico processado para OS %d com %d peças e %d serviços", osId, pecas.size(), servicos.size()));
    }

    @Override
    public void processarOrcamentoAprovado(UUID eventId, Long osId) {
        if (processedEventGateway.jaProcessado(eventId)) {
            BillingEventLogger.eventoDuplicado(osId, eventId, TipoEvento.ORCAMENTO_APROVADO);
            return;
        }

        this.orcamentoUseCase.aprovarOrcamento(osId);
        this.pagamentoUseCase.iniciarPagamento(osId);

        this.processedEventGateway.registrarProcessado(eventId, TipoEvento.ORCAMENTO_APROVADO, String.valueOf(osId));
        BillingEventLogger.eventoProcessado(osId, eventId, TipoEvento.ORCAMENTO_APROVADO,
                "aprovar_orcamento_e_iniciar_pagamento", "sucesso",
                "Orçamento aprovado e cobrança iniciada para OS " + osId);
    }

    @Override
    public void processarExecucaoFalhou(UUID eventId, Long osId, String motivo, String detalhe) {
        if (processedEventGateway.jaProcessado(eventId)) {
            BillingEventLogger.eventoDuplicado(osId, eventId, TipoEvento.EXECUCAO_FALHOU);
            return;
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        boolean pagamentoEstornado = false;

        Optional<Pagamento> pagamentoOpt = this.pagamentoGateway.findUltimoByOsId(osId);
        if (pagamentoOpt.isPresent()) {
            Pagamento pagamento = pagamentoOpt.get();
            if (pagamento.getStatus() == StatusPagamento.APROVADO) {
                pagamento.estornar(agora);
                this.pagamentoGateway.save(pagamento);
                pagamentoEstornado = true;

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
        String motivoTexto = motivo != null ? motivo : "não informado";
        if (pagamentoEstornado) {
            BillingEventLogger.estornoPagamento(osId, eventId, TipoEvento.EXECUCAO_FALHOU,
                    "estornado", String.format("Pagamento estornado após falha na execução da OS %d (motivo: %s)", osId, motivoTexto));
        } else {
            BillingEventLogger.estornoPagamento(osId, eventId, TipoEvento.EXECUCAO_FALHOU,
                    "nao_aplicavel", String.format("Compensação processada sem necessidade de estorno para OS %d (motivo: %s)", osId, motivoTexto));
        }
    }

    @Override
    public void processarOsCancelada(UUID eventId, Long osId, String motivo) {
        if (processedEventGateway.jaProcessado(eventId)) {
            BillingEventLogger.eventoDuplicado(osId, eventId, TipoEvento.OS_CANCELADA);
            return;
        }

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
        String motivoTexto = motivo != null ? motivo : "não informado";
        BillingEventLogger.eventoProcessado(osId, eventId, TipoEvento.OS_CANCELADA,
                "cancelar_os", "sucesso",
                String.format("Cancelamento processado para OS %d (motivo: %s)", osId, motivoTexto));
    }
}
