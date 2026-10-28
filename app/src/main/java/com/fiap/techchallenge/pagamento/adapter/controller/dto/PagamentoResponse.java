package com.fiap.techchallenge.pagamento.adapter.controller.dto;

import com.fiap.techchallenge.pagamento.domain.StatusPagamento;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PagamentoResponse {
    private String id;
    private Long osId;
    private String orcamentoId;
    private BigDecimal valor;
    private StatusPagamento status;
    private String idTransacaoProvedor;
    private String qrCode;
    private String qrCodeBase64;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
}
