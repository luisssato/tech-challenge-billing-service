package com.fiap.techchallenge.servico.domain;

import com.fiap.techchallenge.exception.BadRequestException;

import java.math.BigDecimal;

public class TipoServico {

    private Long id;
    private String nome;
    private BigDecimal valor;

    public TipoServico() {}

    public TipoServico(Long id, String nome, BigDecimal valor) {
        this.id = id;
        this.nome = nome;
        this.valor = valor;
    }

    public static TipoServico criar(String nome, BigDecimal valor) {
        validarNome(nome);
        validarValor(valor);
        return new TipoServico(null, nome, valor);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public BigDecimal getValor() { return valor; }

    public void setId(Long id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public void atualizar(String nome, BigDecimal valor) {
        if (nome != null) {
            validarNome(nome);
            this.nome = nome;
        }

        if (valor != null) {
            validarValor(valor);
            this.valor = valor;
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
