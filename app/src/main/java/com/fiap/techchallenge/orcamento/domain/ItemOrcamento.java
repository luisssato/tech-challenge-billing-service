package com.fiap.techchallenge.orcamento.domain;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.peca.domain.TipoPeca;
import com.fiap.techchallenge.servico.domain.TipoServico;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Linha do orçamento. Guarda um snapshot do preço no momento da geração,
 * de modo que mudanças futuras no catálogo não alterem orçamentos já emitidos.
 */
public class ItemOrcamento {

    private static final int ESCALA_MONETARIA = 2;

    private final TipoItem tipo;
    private final Long idTipo;
    private final String descricao;
    private final int quantidade;
    private final BigDecimal valorUnitario;

    public ItemOrcamento(TipoItem tipo, Long idTipo, String descricao, int quantidade, BigDecimal valorUnitario) {
        if (tipo == null) {
            throw new BadRequestException("Tipo do item do orçamento é obrigatório!");
        }
        if (idTipo == null) {
            throw new BadRequestException("Identificador do tipo de %s é obrigatório!".formatted(tipo));
        }
        if (descricao == null || descricao.isBlank()) {
            throw new BadRequestException("Descrição do item do orçamento é obrigatória!");
        }
        if (quantidade <= 0) {
            throw new BadRequestException("Quantidade do item '%s' deve ser maior que zero!".formatted(descricao));
        }
        if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Valor unitário do item '%s' não pode ser nulo nem negativo!".formatted(descricao));
        }
        this.tipo = tipo;
        this.idTipo = idTipo;
        this.descricao = descricao;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
    }

    public static ItemOrcamento deTipoPeca(TipoPeca tipoPeca, int quantidade) {
        return new ItemOrcamento(TipoItem.PECA, tipoPeca.getId(), tipoPeca.getNome(), quantidade, tipoPeca.getValorUnitario());
    }

    public static ItemOrcamento deTipoServico(TipoServico tipoServico, int quantidade) {
        return new ItemOrcamento(TipoItem.SERVICO, tipoServico.getId(), tipoServico.getNome(), quantidade, tipoServico.getValor());
    }

    public BigDecimal getSubtotal() {
        return valorUnitario
                .multiply(BigDecimal.valueOf(quantidade))
                .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    public TipoItem getTipo() { return tipo; }
    public Long getIdTipo() { return idTipo; }
    public String getDescricao() { return descricao; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
}
