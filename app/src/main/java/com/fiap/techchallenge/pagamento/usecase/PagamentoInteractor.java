package com.fiap.techchallenge.pagamento.usecase;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.exception.NotFoundException;
import com.fiap.techchallenge.mensageria.domain.TipoEvento;
import com.fiap.techchallenge.mensageria.usecase.EventoSaidaGateway;
import com.fiap.techchallenge.observabilidade.BillingEventLogger;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.domain.StatusOrcamento;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoGateway;
import com.fiap.techchallenge.pagamento.domain.Pagamento;
import com.fiap.techchallenge.pagamento.domain.StatusPagamento;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
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
                BillingEventLogger.pagamento(osId, "iniciar_pagamento", "reaproveitado",
                        "Cobrança pendente reaproveitada para OS " + osId + " (transação " + ultimo.getIdTransacaoProvedor() + ")");
                return ultimo;
            }
        }

        Pagamento pagamento = Pagamento.criar(osId, orcamento.getId(), orcamento.getValorTotal(), agora);
        CobrancaQrCode cobranca = this.provedorPagamentoGateway.criarCobranca(
                osId, orcamento.getId(), orcamento.getValorTotal(), "Pagamento Ordem de Serviço " + osId);

        pagamento.registrarCobranca(cobranca.idTransacao(), cobranca.qrCode(), cobranca.qrCodeBase64(), agora);
        Pagamento salvo = this.pagamentoGateway.save(pagamento);
        BillingEventLogger.pagamento(osId, "iniciar_pagamento", "criado",
                "Cobrança iniciada para OS " + osId + " (transação " + cobranca.idTransacao() + ")");
        return salvo;
    }

    @Override
    public Optional<Pagamento> processarNotificacaoWebhook(String idTransacaoProvedor) {
        if (idTransacaoProvedor == null || idTransacaoProvedor.isBlank()) {
            throw new BadRequestException("Erro: O identificador da transação é obrigatório para processar o webhook!");
        }

        Optional<Pagamento> pagamentoOpt = this.pagamentoGateway.findByIdTransacaoProvedor(idTransacaoProvedor);
        if (pagamentoOpt.isEmpty()) {
            BillingEventLogger.pagamentoAlerta(null, "processar_webhook", "nao_encontrado",
                    "Nenhuma cobrança local vinculada à transação " + idTransacaoProvedor);
            return Optional.empty();
        }

        Pagamento pagamento = pagamentoOpt.get();
        if (pagamento.getStatus() == StatusPagamento.APROVADO) {
            BillingEventLogger.pagamento(pagamento.getOsId(), "processar_webhook", "idempotente",
                    "Pagamento da OS " + pagamento.getOsId() + " já aprovado anteriormente");
            return Optional.of(pagamento);
        }

        ConsultaPagamento consulta = this.provedorPagamentoGateway.consultarPagamento(idTransacaoProvedor);
        LocalDateTime agora = agora();

        String outcome;
        String mensagem;
        switch (consulta.status()) {
            case APROVADO -> {
                pagamento.confirmar(idTransacaoProvedor, agora);
                outcome = "confirmado";
                mensagem = "Pagamento confirmado para OS " + pagamento.getOsId() + " (transação " + idTransacaoProvedor + ")";
                this.eventoSaidaGateway.publicarEvento(TipoEvento.PAGAMENTO_CONFIRMADO, String.valueOf(pagamento.getOsId()), Map.of(
                        "osId", pagamento.getOsId(),
                        "orcamentoId", pagamento.getOrcamentoId() != null ? pagamento.getOrcamentoId() : "",
                        "pagamentoId", idTransacaoProvedor,
                        "valor", pagamento.getValor()
                ));
            }
            case RECUSADO -> {
                pagamento.recusar(idTransacaoProvedor, agora);
                outcome = "recusado";
                mensagem = "Pagamento recusado para OS " + pagamento.getOsId() + " (transação " + idTransacaoProvedor + ")";
                this.eventoSaidaGateway.publicarEvento(TipoEvento.PAGAMENTO_FALHOU, String.valueOf(pagamento.getOsId()), Map.of(
                        "osId", String.valueOf(pagamento.getOsId()),
                        "pagamentoId", idTransacaoProvedor,
                        "motivo", "PAGAMENTO_RECUSADO"
                ));
            }
            case CANCELADO -> {
                pagamento.cancelar(agora);
                outcome = "cancelado";
                mensagem = "Pagamento cancelado para OS " + pagamento.getOsId() + " (transação " + idTransacaoProvedor + ")";
            }
            default -> {
                outcome = "inalterado";
                mensagem = "Pagamento da OS " + pagamento.getOsId() + " permanece no status " + pagamento.getStatus();
            }
        }

        Pagamento salvo = this.pagamentoGateway.save(pagamento);
        BillingEventLogger.pagamento(pagamento.getOsId(), "processar_webhook", outcome, mensagem);
        return Optional.of(salvo);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
