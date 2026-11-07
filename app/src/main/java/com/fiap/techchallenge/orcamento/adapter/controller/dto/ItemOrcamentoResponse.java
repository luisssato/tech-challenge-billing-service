package com.fiap.techchallenge.orcamento.adapter.controller.dto;

import com.fiap.techchallenge.orcamento.domain.TipoItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemOrcamentoResponse {
    private TipoItem tipo;
    private Long idTipo;
    private String descricao;
    private int quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal subtotal;
}
