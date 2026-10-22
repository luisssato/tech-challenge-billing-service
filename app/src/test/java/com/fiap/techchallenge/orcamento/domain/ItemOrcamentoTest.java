package com.fiap.techchallenge.orcamento.domain;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.peca.domain.TipoPeca;
import com.fiap.techchallenge.servico.domain.TipoServico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class ItemOrcamentoTest {

    @Test
    public void given_dadosValidos_when_criaItem_then_guardaSnapshotDoPreco() {
        ItemOrcamento item = new ItemOrcamento(TipoItem.PECA, 7L, "Pastilha de freio", 2, new BigDecimal("50.00"));

        assertEquals(TipoItem.PECA, item.getTipo());
        assertEquals(7L, item.getIdTipo());
        assertEquals("Pastilha de freio", item.getDescricao());
        assertEquals(2, item.getQuantidade());
        assertEquals(new BigDecimal("50.00"), item.getValorUnitario());
    }

    @Test
    public void given_quantidadeEValorUnitario_when_calculaSubtotal_then_multiplica() {
        ItemOrcamento item = new ItemOrcamento(TipoItem.PECA, 1L, "Filtro", 3, new BigDecimal("12.50"));

        assertEquals(new BigDecimal("37.50"), item.getSubtotal());
    }

    @Test
    public void given_valorComMaisDeDuasCasas_when_calculaSubtotal_then_arredondaHalfUp() {
        ItemOrcamento item = new ItemOrcamento(TipoItem.SERVICO, 1L, "Alinhamento", 1, new BigDecimal("10.005"));

        assertEquals(new BigDecimal("10.01"), item.getSubtotal());
    }

    @Test
    public void given_valorZero_when_criaItem_then_permiteItemGratuito() {
        ItemOrcamento item = new ItemOrcamento(TipoItem.SERVICO, 1L, "Cortesia", 1, BigDecimal.ZERO);

        assertEquals(new BigDecimal("0.00"), item.getSubtotal());
    }

    @Test
    public void given_tipoNulo_when_criaItem_then_lancaBadRequest() {
        assertThrows(BadRequestException.class,
                () -> new ItemOrcamento(null, 1L, "Filtro", 1, BigDecimal.TEN));
    }

    @Test
    public void given_idTipoNulo_when_criaItem_then_lancaBadRequest() {
        assertThrows(BadRequestException.class,
                () -> new ItemOrcamento(TipoItem.PECA, null, "Filtro", 1, BigDecimal.TEN));
    }

    @Test
    public void given_descricaoNulaOuEmBranco_when_criaItem_then_lancaBadRequest() {
        assertThrows(BadRequestException.class,
                () -> new ItemOrcamento(TipoItem.PECA, 1L, null, 1, BigDecimal.TEN));
        assertThrows(BadRequestException.class,
                () -> new ItemOrcamento(TipoItem.PECA, 1L, "  ", 1, BigDecimal.TEN));
    }

    @Test
    public void given_quantidadeZeroOuNegativa_when_criaItem_then_lancaBadRequest() {
        assertThrows(BadRequestException.class,
                () -> new ItemOrcamento(TipoItem.PECA, 1L, "Filtro", 0, BigDecimal.TEN));
        assertThrows(BadRequestException.class,
                () -> new ItemOrcamento(TipoItem.PECA, 1L, "Filtro", -1, BigDecimal.TEN));
    }

    @Test
    public void given_valorUnitarioNuloOuNegativo_when_criaItem_then_lancaBadRequest() {
        assertThrows(BadRequestException.class,
                () -> new ItemOrcamento(TipoItem.PECA, 1L, "Filtro", 1, null));
        assertThrows(BadRequestException.class,
                () -> new ItemOrcamento(TipoItem.PECA, 1L, "Filtro", 1, new BigDecimal("-0.01")));
    }

    @Test
    public void given_tipoPeca_when_criaItemDeTipoPeca_then_copiaIdNomeEValor() {
        TipoPeca tipoPeca = new TipoPeca(3L, "Óleo 5W30", new BigDecimal("35.90"));

        ItemOrcamento item = ItemOrcamento.deTipoPeca(tipoPeca, 4);

        assertEquals(TipoItem.PECA, item.getTipo());
        assertEquals(3L, item.getIdTipo());
        assertEquals("Óleo 5W30", item.getDescricao());
        assertEquals(4, item.getQuantidade());
        assertEquals(new BigDecimal("35.90"), item.getValorUnitario());
    }

    @Test
    public void given_tipoServico_when_criaItemDeTipoServico_then_copiaIdNomeEValor() {
        TipoServico tipoServico = new TipoServico(9L, "Troca de óleo", new BigDecimal("80.00"));

        ItemOrcamento item = ItemOrcamento.deTipoServico(tipoServico, 1);

        assertEquals(TipoItem.SERVICO, item.getTipo());
        assertEquals(9L, item.getIdTipo());
        assertEquals("Troca de óleo", item.getDescricao());
        assertEquals(1, item.getQuantidade());
        assertEquals(new BigDecimal("80.00"), item.getValorUnitario());
    }
}
