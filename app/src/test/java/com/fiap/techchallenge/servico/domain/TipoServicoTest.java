package com.fiap.techchallenge.servico.domain;

import com.fiap.techchallenge.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class TipoServicoTest {

    @Test
    public void given_dadosValidos_when_cria_then_semIdEComDadosInformados() {
        TipoServico tipoServico = TipoServico.criar("Troca de óleo", new BigDecimal("80.00"));

        assertNull(tipoServico.getId());
        assertEquals("Troca de óleo", tipoServico.getNome());
        assertEquals(new BigDecimal("80.00"), tipoServico.getValor());
    }

    @Test
    public void given_nomeVazio_when_cria_then_lancaBadRequest() {
        assertThrows(BadRequestException.class, () -> TipoServico.criar(" ", BigDecimal.TEN));
        assertThrows(BadRequestException.class, () -> TipoServico.criar(null, BigDecimal.TEN));
    }

    @Test
    public void given_valorNuloOuNegativo_when_cria_then_lancaBadRequest() {
        assertThrows(BadRequestException.class, () -> TipoServico.criar("Alinhamento", null));
        assertThrows(BadRequestException.class, () -> TipoServico.criar("Alinhamento", new BigDecimal("-1")));
    }

    @Test
    public void given_valorZero_when_cria_then_permite() {
        assertDoesNotThrow(() -> TipoServico.criar("Cortesia", BigDecimal.ZERO));
    }

    @Test
    public void given_novosDados_when_atualiza_then_alteraNomeEValor() {
        TipoServico tipoServico = new TipoServico(1L, "Alinhamento", BigDecimal.TEN);

        tipoServico.atualizar("Balanceamento", new BigDecimal("60.00"));

        assertEquals("Balanceamento", tipoServico.getNome());
        assertEquals(new BigDecimal("60.00"), tipoServico.getValor());
    }

    @Test
    public void given_camposNulos_when_atualiza_then_mantemValoresAtuais() {
        TipoServico tipoServico = new TipoServico(1L, "Alinhamento", BigDecimal.TEN);

        tipoServico.atualizar(null, null);

        assertEquals("Alinhamento", tipoServico.getNome());
        assertEquals(BigDecimal.TEN, tipoServico.getValor());
    }

    @Test
    public void given_nomeEmBranco_when_atualiza_then_lancaBadRequestSemAlterar() {
        TipoServico tipoServico = new TipoServico(1L, "Alinhamento", BigDecimal.TEN);

        assertThrows(BadRequestException.class, () -> tipoServico.atualizar(" ", null));

        assertEquals("Alinhamento", tipoServico.getNome());
    }

    @Test
    public void given_valorNegativo_when_atualiza_then_lancaBadRequestSemAlterar() {
        TipoServico tipoServico = new TipoServico(1L, "Alinhamento", BigDecimal.TEN);

        assertThrows(BadRequestException.class, () -> tipoServico.atualizar(null, new BigDecimal("-5")));

        assertEquals(BigDecimal.TEN, tipoServico.getValor());
    }

    @Test
    public void given_setters_when_preenche_then_getters_retornamValores() {
        TipoServico tipoServico = new TipoServico();
        tipoServico.setId(5L);
        tipoServico.setNome("Revisão");
        tipoServico.setValor(new BigDecimal("200.00"));

        assertEquals(5L, tipoServico.getId());
        assertEquals("Revisão", tipoServico.getNome());
        assertEquals(new BigDecimal("200.00"), tipoServico.getValor());
    }
}
