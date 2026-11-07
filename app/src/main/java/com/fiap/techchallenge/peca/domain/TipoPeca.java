package com.fiap.techchallenge.peca.domain;

import com.fiap.techchallenge.exception.BadRequestException;

import java.math.BigDecimal;

public class TipoPeca {

    private Long id;
    private String nome;
    private BigDecimal valorUnitario;

    public TipoPeca() {}

    public TipoPeca(Long id, String nome, BigDecimal valorUnitario) {
        this.id = id;
        this.nome = nome;
        this.valorUnitario = valorUnitario;
    }

    public static TipoPeca criar(String nome, BigDecimal valorUnitario) {
        validarNome(nome);
        validarValor(valorUnitario);
        return new TipoPeca(null, nome, valorUnitario);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public BigDecimal getValorUnitario() { return valorUnitario; }

    public void setId(Long id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }

    public void atualizar(String nome, BigDecimal valorUnitario) {
        if (nome != null) {
            validarNome(nome);
            this.nome = nome;
        }

        if (valorUnitario != null) {
            validarValor(valorUnitario);
            this.valorUnitario = valorUnitario;
        }
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BadRequestException("Nome não pode ser vazio");
        }
    }

    private static void validarValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Valor não pode ser nulo nem negativo");
        }
    }
}
