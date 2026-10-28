package com.fiap.techchallenge.pagamento.usecase;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.exception.NotFoundException;
import com.fiap.techchallenge.mensageria.domain.TipoEvento;
import com.fiap.techchallenge.mensageria.usecase.EventoSaidaGateway;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.domain.StatusOrcamento;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoGateway;
import com.fiap.techchallenge.pagamento.domain.Pagamento;
import com.fiap.techchallenge.pagamento.domain.StatusPagamento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class PagamentoInteractor implements PagamentoUseCase {

    private final PagamentoGateway pagamentoGateway;
    private final OrcamentoGateway orcamentoGateway;
    private final ProvedorPagamentoGateway provedorPagamentoGateway;
    private final EventoSaidaGateway eventoSaidaGateway;
    private final Clock clock;

    public PagamentoInteractor(PagamentoGateway pagamentoGateway,
                               OrcamentoGateway orcamentoGateway,
                               ProvedorPagamentoGateway provedorPagamentoGateway,
                               EventoSaidaGateway eventoSaidaGateway,
                               Clock clock) {
        this.pagamentoGateway = pagamentoGateway;
        this.orcamentoGateway = orcamentoGateway;
        this.provedorPagamentoGateway = provedorPagamentoGateway;
        this.eventoSaidaGateway = eventoSaidaGateway;
        this.clock = clock;
    }

    @Override
    public Optional<Pagamento> getPagamentoPorOs(Long osId) {
        return this.pagamentoGateway.findUltimoByOsId(osId);
    }

    @Override
    public Pagamento iniciarPagamento(Long osId) {
        if (osId == null) {
            throw new BadRequestException("Erro: O pagamento deve estar vinculado a uma Ordem de Serviço!");
        }

        Orcamento orcamento = this.orcamentoGateway.findByOsId(osId)
                .orElseThrow(() -> new NotFoundException("Orçamento da ordem de serviço de id " + osId + " não encontrado!"));

        if (orcamento.getStatus() != StatusOrcamento.APROVADO) {
            throw new ConflictException(
                    "Erro: O orçamento da Ordem de Serviço %d deve estar no status '%s' para iniciar o pagamento! Status atual: %s"
                            .formatted(osId, StatusOrcamento.APROVADO, orcamento.getStatus()));
        }

        LocalDateTime agora = agora();

        Optional<Pagamento> ultimoOpt = this.pagamentoGateway.findUltimoByOsId(osId);
        if (ultimoOpt.isPresent()) {
            Pagamento ultimo = ultimoOpt.get();
            if (ultimo.getStatus() == StatusPagamento.APROVADO) {
                throw new ConflictException("Erro: O pagamento da Ordem de Serviço %d já foi aprovado!".formatted(osId));
            }
            if ((ultimo.getStatus() == StatusPagamento.PENDENTE || ultimo.getStatus() == StatusPagamento.PROCESSANDO)
                    && ultimo.getQrCode() != null) {
                log.info("Reaproveitando cobrança pendente para a OS {}: transacao={}", osId, ultimo.getIdTransacaoProvedor());
                return ultimo;
            }
        }

        Pagamento pagamento = Pagamento.criar(osId, orcamento.getId(), orcamento.getValorTotal(), agora);
        CobrancaQrCode cobranca = this.provedorPagamentoGateway.criarCobranca(
                osId, orcamento.getId(), orcamento.getValorTotal(), "Pagamento Ordem de Serviço " + osId);

        pagamento.registrarCobranca(cobranca.idTransacao(), cobranca.qrCode(), cobranca.qrCodeBase64(), agora);
        return this.pagamentoGateway.save(pagamento);
    }

    @Override
    public Optional<Pagamento> processarNotificacaoWebhook(String idTransacaoProvedor) {
        if (idTransacaoProvedor == null || idTransacaoProvedor.isBlank()) {
            throw new BadRequestException("Erro: O identificador da transação é obrigatório para processar o webhook!");
        }

        Optional<Pagamento> pagamentoOpt = this.pagamentoGateway.findByIdTransacaoProvedor(idTransacaoProvedor);
        if (pagamentoOpt.isEmpty()) {
            log.warn("[WEBHOOK] Nenhuma cobrança local vinculada à transação {}", idTransacaoProvedor);
            return Optional.empty();
        }

        Pagamento pagamento = pagamentoOpt.get();
        if (pagamento.getStatus() == StatusPagamento.APROVADO) {
            log.info("[WEBHOOK] Pagamento da OS {} já aprovado anteriormente (idempotente).", pagamento.getOsId());
            return Optional.of(pagamento);
        }

        ConsultaPagamento consulta = this.provedorPagamentoGateway.consultarPagamento(idTransacaoProvedor);
        LocalDateTime agora = agora();

        switch (consulta.status()) {
            case APROVADO -> {
                log.info("[WEBHOOK] Confirmando pagamento da OS {} (transação {})", pagamento.getOsId(), idTransacaoProvedor);
                pagamento.confirmar(idTransacaoProvedor, agora);
                this.eventoSaidaGateway.publicarEvento(TipoEvento.PAGAMENTO_CONFIRMADO, String.valueOf(pagamento.getOsId()), Map.of(
                        "osId", pagamento.getOsId(),
                        "orcamentoId", pagamento.getOrcamentoId() != null ? pagamento.getOrcamentoId() : "",
                        "pagamentoId", idTransacaoProvedor,
                        "valor", pagamento.getValor()
                ));
            }
            case RECUSADO -> {
                log.info("[WEBHOOK] Recusando pagamento da OS {} (transação {})", pagamento.getOsId(), idTransacaoProvedor);
                pagamento.recusar(idTransacaoProvedor, agora);
                this.eventoSaidaGateway.publicarEvento(TipoEvento.PAGAMENTO_RECUSADO, String.valueOf(pagamento.getOsId()), Map.of(
                        "osId", pagamento.getOsId(),
                        "pagamentoId", idTransacaoProvedor
                ));
            }
            case CANCELADO -> {
                log.info("[WEBHOOK] Cancelando pagamento da OS {} (transação {})", pagamento.getOsId(), idTransacaoProvedor);
                pagamento.cancelar(agora);
            }
            default -> log.info("[WEBHOOK] Pagamento da OS {} permanece no status {}", pagamento.getOsId(), pagamento.getStatus());
        }

        return Optional.of(this.pagamentoGateway.save(pagamento));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
