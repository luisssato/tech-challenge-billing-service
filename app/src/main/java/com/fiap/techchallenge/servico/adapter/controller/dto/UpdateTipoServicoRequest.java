package com.fiap.techchallenge.servico.adapter.controller.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateTipoServicoRequest {
    private String nome;
    private BigDecimal valor;
}
