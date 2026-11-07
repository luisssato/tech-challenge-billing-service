package com.fiap.techchallenge.servico.adapter.controller.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TipoServicoResponse {
    private Long id;
    private String nome;
    private BigDecimal valor;
}
