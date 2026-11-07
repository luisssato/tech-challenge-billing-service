package com.fiap.techchallenge.peca.adapter.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTipoPecaRequest {

    private Long id;

    @NotBlank(message = "Nome não pode ser nulo!")
    private String nome;

    @NotNull(message = "Valor Unitário não pode ser nulo!")
    private BigDecimal valorUnitario;
}
