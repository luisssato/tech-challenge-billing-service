package com.fiap.techchallenge.peca.adapter.controller.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TipoPecaResponse {
    private Long id;
    private String nome;
    private BigDecimal valorUnitario;
}
