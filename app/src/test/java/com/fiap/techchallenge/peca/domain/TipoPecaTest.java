package com.fiap.techchallenge.peca.domain;

import com.fiap.techchallenge.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class TipoPecaTest {

    @Test
    public void given_dadosValidos_when_cria_then_semIdEComDadosInformados() {
        TipoPeca tipoPeca = TipoPeca.criar("Filtro de óleo", new BigDecimal("25.00"));

        assertNull(tipoPeca.getId());
        assertEquals("Filtro de óleo", tipoPeca.getNome());
        assertEquals(new BigDecimal("25.00"), tipoPeca.getValorUnitario());
    }

    @Test
    public void given_nomeVazio_when_cria_then_lancaBadRequest() {
        assertThrows(BadRequestException.class, () -> TipoPeca.criar(" ", BigDecimal.TEN));
        assertThrows(BadRequestException.class, () -> TipoPeca.criar(null, BigDecimal.TEN));
    }

    @Test
    public void given_valorNuloOuNegativo_when_cria_then_lancaBadRequest() {
        assertThrows(BadRequestException.class, () -> TipoPeca.criar("Filtro", null));
        assertThrows(BadRequestException.class, () -> TipoPeca.criar("Filtro", new BigDecimal("-1")));
    }

    @Test
    public void given_valorZero_when_cria_then_permite() {
        assertDoesNotThrow(() -> TipoPeca.criar("Brinde", BigDecimal.ZERO));
    }

    @Test
    public void given_novosDados_when_atualiza_then_alteraNomeEValor() {
        TipoPeca tipoPeca = new TipoPeca(1L, "Filtro", BigDecimal.TEN);

        tipoPeca.atualizar("Filtro de ar", new BigDecimal("30.00"));

        assertEquals("Filtro de ar", tipoPeca.getNome());
        assertEquals(new BigDecimal("30.00"), tipoPeca.getValorUnitario());
    }

    @Test
    public void given_camposNulos_when_atualiza_then_mantemValoresAtuais() {
        TipoPeca tipoPeca = new TipoPeca(1L, "Filtro", BigDecimal.TEN);

        tipoPeca.atualizar(null, null);

        assertEquals("Filtro", tipoPeca.getNome());
        assertEquals(BigDecimal.TEN, tipoPeca.getValorUnitario());
    }

    @Test
    public void given_nomeEmBranco_when_atualiza_then_lancaBadRequestSemAlterar() {
        TipoPeca tipoPeca = new TipoPeca(1L, "Filtro", BigDecimal.TEN);

        assertThrows(BadRequestException.class, () -> tipoPeca.atualizar(" ", null));

        assertEquals("Filtro", tipoPeca.getNome());
    }

    @Test
    public void given_valorNegativo_when_atualiza_then_lancaBadRequestSemAlterar() {
        TipoPeca tipoPeca = new TipoPeca(1L, "Filtro", BigDecimal.TEN);

        assertThrows(BadRequestException.class, () -> tipoPeca.atualizar(null, new BigDecimal("-5")));

        assertEquals(BigDecimal.TEN, tipoPeca.getValorUnitario());
    }

    @Test
    public void given_setters_when_preenche_then_getters_retornamValores() {
        TipoPeca tipoPeca = new TipoPeca();
        tipoPeca.setId(5L);
        tipoPeca.setNome("Vela");
        tipoPeca.setValorUnitario(new BigDecimal("12.00"));

        assertEquals(5L, tipoPeca.getId());
        assertEquals("Vela", tipoPeca.getNome());
        assertEquals(new BigDecimal("12.00"), tipoPeca.getValorUnitario());
    }
}
