package com.fiap.techchallenge.orcamento.adapter.controller.dto;

import com.fiap.techchallenge.orcamento.domain.StatusOrcamento;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrcamentoResponse {
    private String id;
    private Long osId;
    private StatusOrcamento status;
    private List<ItemOrcamentoResponse> itens;
    private BigDecimal valorTotalPecas;
    private BigDecimal valorTotalServicos;
    private BigDecimal valorTotal;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataExpiracao;
    private LocalDateTime dataAtualizacao;
}
