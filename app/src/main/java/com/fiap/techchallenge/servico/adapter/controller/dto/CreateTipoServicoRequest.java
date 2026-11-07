package com.fiap.techchallenge.servico.adapter.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTipoServicoRequest {

    private Long id;

    @NotBlank(message = "Nome não pode ser nulo!")
    private String nome;

    @NotNull(message = "Valor não pode ser nulo!")
    private BigDecimal valor;
}
