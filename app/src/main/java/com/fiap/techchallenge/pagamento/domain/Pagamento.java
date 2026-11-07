package com.fiap.techchallenge.pagamento.domain;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.exception.ConflictException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

public class Pagamento {

    private static final Set<StatusPagamento> EM_ABERTO = EnumSet.of(StatusPagamento.PENDENTE, StatusPagamento.PROCESSANDO);

    private final String id;
    private final Long osId;
    private final String orcamentoId;
    private final BigDecimal valor;
    private StatusPagamento status;
    private String idTransacaoProvedor;
    private String qrCode;
    private String qrCodeBase64;
    private final LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    public Pagamento(String id, Long osId, String orcamentoId, BigDecimal valor, StatusPagamento status,
                     String idTransacaoProvedor, String qrCode, String qrCodeBase64,
                     LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
        this.id = id;
        this.osId = osId;
        this.orcamentoId = orcamentoId;
        this.valor = valor;
        this.status = status;
        this.idTransacaoProvedor = idTransacaoProvedor;
        this.qrCode = qrCode;
        this.qrCodeBase64 = qrCodeBase64;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
    }

    public static Pagamento criar(Long osId, String orcamentoId, BigDecimal valor, LocalDateTime agora) {
        if (osId == null) {
            throw new BadRequestException("Erro: O pagamento deve estar vinculado a uma Ordem de Serviço!");
        }
        if (orcamentoId == null || orcamentoId.isBlank()) {
            throw new BadRequestException("Erro: O pagamento deve estar vinculado a um orçamento!");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Erro: O valor do pagamento deve ser maior que zero!");
        }
        return new Pagamento(null, osId, orcamentoId, valor, StatusPagamento.PENDENTE,
                null, null, null, agora, agora);
    }

    public void registrarCobranca(String idTransacaoProvedor, String qrCode, String qrCodeBase64, LocalDateTime agora) {
        exigirStatus("registrar a cobrança", EnumSet.of(StatusPagamento.PENDENTE));
        if (idTransacaoProvedor == null || idTransacaoProvedor.isBlank()) {
            throw new BadRequestException("Erro: O identificador da transação no provedor é obrigatório!");
        }
        this.idTransacaoProvedor = idTransacaoProvedor;
        this.qrCode = qrCode;
        this.qrCodeBase64 = qrCodeBase64;
        this.dataAtualizacao = agora;
    }

    public void iniciarProcessamento(LocalDateTime agora) {
        exigirStatus("entrar em processamento", EnumSet.of(StatusPagamento.PENDENTE));
        transicionarPara(StatusPagamento.PROCESSANDO, agora);
    }

    public void confirmar(String idTransacaoProvedor, LocalDateTime agora) {
        exigirStatus("ser confirmado", EM_ABERTO);
        atualizarIdTransacao(idTransacaoProvedor);
        transicionarPara(StatusPagamento.APROVADO, agora);
    }

    public void recusar(String idTransacaoProvedor, LocalDateTime agora) {
        exigirStatus("ser recusado", EM_ABERTO);
        atualizarIdTransacao(idTransacaoProvedor);
        transicionarPara(StatusPagamento.RECUSADO, agora);
    }

    public void cancelar(LocalDateTime agora) {
        exigirStatus("ser cancelado", EM_ABERTO);
        transicionarPara(StatusPagamento.CANCELADO, agora);
    }

    public void estornar(LocalDateTime agora) {
        exigirStatus("ser estornado", EnumSet.of(StatusPagamento.APROVADO));
        transicionarPara(StatusPagamento.ESTORNADO, agora);
    }

    private void atualizarIdTransacao(String idTransacaoProvedor) {
        if (idTransacaoProvedor != null && !idTransacaoProvedor.isBlank()) {
            this.idTransacaoProvedor = idTransacaoProvedor;
        }
    }

    private void exigirStatus(String acao, Set<StatusPagamento> permitidos) {
        if (!permitidos.contains(this.status)) {
            throw new ConflictException(
                    "Erro: O pagamento da Ordem de Serviço %d está no status '%s' e não pode %s! Status permitidos: %s"
                            .formatted(osId, status, acao, permitidos));
        }
    }

    private void transicionarPara(StatusPagamento novoStatus, LocalDateTime agora) {
        this.status = novoStatus;
        this.dataAtualizacao = agora;
    }

    public String getId() { return id; }
    public Long getOsId() { return osId; }
    public String getOrcamentoId() { return orcamentoId; }
    public BigDecimal getValor() { return valor; }
    public StatusPagamento getStatus() { return status; }
    public String getIdTransacaoProvedor() { return idTransacaoProvedor; }
    public String getQrCode() { return qrCode; }
    public String getQrCodeBase64() { return qrCodeBase64; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
}
