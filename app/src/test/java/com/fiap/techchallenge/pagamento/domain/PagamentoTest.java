package com.fiap.techchallenge.pagamento.domain;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class PagamentoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 21, 10, 0);
    private static final LocalDateTime DEPOIS = AGORA.plusMinutes(5);

    private Pagamento pagamentoPendente() {
        return Pagamento.criar(10L, "orc-1", new BigDecimal("180.00"), AGORA);
    }

    private Pagamento pagamentoComStatus(StatusPagamento status) {
        return new Pagamento("pag-1", 10L, "orc-1", new BigDecimal("180.00"), status,
                "mp-123", null, null, AGORA, AGORA);
    }

    // criar()

    @Test
    public void given_dadosValidos_when_criaPagamento_then_statusPendenteSemTransacao() {
        Pagamento pagamento = pagamentoPendente();

        assertNull(pagamento.getId());
        assertEquals(10L, pagamento.getOsId());
        assertEquals("orc-1", pagamento.getOrcamentoId());
        assertEquals(new BigDecimal("180.00"), pagamento.getValor());
        assertEquals(StatusPagamento.PENDENTE, pagamento.getStatus());
        assertNull(pagamento.getIdTransacaoProvedor());
        assertNull(pagamento.getQrCode());
        assertNull(pagamento.getQrCodeBase64());
        assertEquals(AGORA, pagamento.getDataCriacao());
        assertEquals(AGORA, pagamento.getDataAtualizacao());
    }

    @Test
    public void given_osIdNulo_when_criaPagamento_then_lancaBadRequest() {
        assertThrows(BadRequestException.class,
                () -> Pagamento.criar(null, "orc-1", BigDecimal.TEN, AGORA));
    }

    @Test
    public void given_orcamentoIdNuloOuEmBranco_when_criaPagamento_then_lancaBadRequest() {
        assertThrows(BadRequestException.class,
                () -> Pagamento.criar(10L, null, BigDecimal.TEN, AGORA));
        assertThrows(BadRequestException.class,
                () -> Pagamento.criar(10L, " ", BigDecimal.TEN, AGORA));
    }

    @Test
    public void given_valorNuloZeroOuNegativo_when_criaPagamento_then_lancaBadRequest() {
        assertThrows(BadRequestException.class,
                () -> Pagamento.criar(10L, "orc-1", null, AGORA));
        assertThrows(BadRequestException.class,
                () -> Pagamento.criar(10L, "orc-1", BigDecimal.ZERO, AGORA));
        assertThrows(BadRequestException.class,
                () -> Pagamento.criar(10L, "orc-1", new BigDecimal("-1.00"), AGORA));
    }

    // registrarCobranca()

    @Test
    public void given_pagamentoPendente_when_registraCobranca_then_guardaDadosDoProvedorMantendoPendente() {
        Pagamento pagamento = pagamentoPendente();

        pagamento.registrarCobranca("mp-123", "00020126...", "base64==", DEPOIS);

        assertEquals(StatusPagamento.PENDENTE, pagamento.getStatus());
        assertEquals("mp-123", pagamento.getIdTransacaoProvedor());
        assertEquals("00020126...", pagamento.getQrCode());
        assertEquals("base64==", pagamento.getQrCodeBase64());
        assertEquals(DEPOIS, pagamento.getDataAtualizacao());
    }

    @Test
    public void given_idTransacaoNuloOuEmBranco_when_registraCobranca_then_lancaBadRequest() {
        Pagamento pagamento = pagamentoPendente();

        assertThrows(BadRequestException.class, () -> pagamento.registrarCobranca(null, "qr", "b64", DEPOIS));
        assertThrows(BadRequestException.class, () -> pagamento.registrarCobranca(" ", "qr", "b64", DEPOIS));
        assertNull(pagamento.getIdTransacaoProvedor());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPagamento.class, names = "PENDENTE", mode = EnumSource.Mode.EXCLUDE)
    public void given_pagamentoForaDePendente_when_registraCobranca_then_lancaConflict(StatusPagamento status) {
        Pagamento pagamento = pagamentoComStatus(status);

        assertThrows(ConflictException.class, () -> pagamento.registrarCobranca("mp-999", "qr", "b64", DEPOIS));
        assertEquals("mp-123", pagamento.getIdTransacaoProvedor());
    }

    // iniciarProcessamento()

    @Test
    public void given_pagamentoPendente_when_iniciaProcessamento_then_statusProcessando() {
        Pagamento pagamento = pagamentoPendente();

        pagamento.iniciarProcessamento(DEPOIS);

        assertEquals(StatusPagamento.PROCESSANDO, pagamento.getStatus());
        assertEquals(DEPOIS, pagamento.getDataAtualizacao());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPagamento.class, names = "PENDENTE", mode = EnumSource.Mode.EXCLUDE)
    public void given_pagamentoForaDePendente_when_iniciaProcessamento_then_lancaConflict(StatusPagamento status) {
        Pagamento pagamento = pagamentoComStatus(status);

        assertThrows(ConflictException.class, () -> pagamento.iniciarProcessamento(DEPOIS));
        assertEquals(status, pagamento.getStatus());
    }

    // confirmar()

    @ParameterizedTest
    @EnumSource(value = StatusPagamento.class, names = {"PENDENTE", "PROCESSANDO"})
    public void given_pagamentoEmAberto_when_confirma_then_statusAprovadoComIdDaTransacao(StatusPagamento status) {
        Pagamento pagamento = pagamentoComStatus(status);

        pagamento.confirmar("mp-777", DEPOIS);

        assertEquals(StatusPagamento.APROVADO, pagamento.getStatus());
        assertEquals("mp-777", pagamento.getIdTransacaoProvedor());
        assertEquals(DEPOIS, pagamento.getDataAtualizacao());
    }

    @Test
    public void given_idTransacaoEmBranco_when_confirma_then_mantemIdAnterior() {
        Pagamento pagamento = pagamentoComStatus(StatusPagamento.PROCESSANDO);

        pagamento.confirmar(" ", DEPOIS);

        assertEquals(StatusPagamento.APROVADO, pagamento.getStatus());
        assertEquals("mp-123", pagamento.getIdTransacaoProvedor());
    }

    @Test
    public void given_idTransacaoNulo_when_confirma_then_mantemIdAnterior() {
        Pagamento pagamento = pagamentoComStatus(StatusPagamento.PENDENTE);

        pagamento.confirmar(null, DEPOIS);

        assertEquals("mp-123", pagamento.getIdTransacaoProvedor());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPagamento.class, names = {"APROVADO", "RECUSADO", "CANCELADO", "ESTORNADO"})
    public void given_pagamentoFinalizado_when_confirma_then_lancaConflict(StatusPagamento status) {
        Pagamento pagamento = pagamentoComStatus(status);

        assertThrows(ConflictException.class, () -> pagamento.confirmar("mp-777", DEPOIS));
        assertEquals(status, pagamento.getStatus());
        assertEquals("mp-123", pagamento.getIdTransacaoProvedor());
    }

    // recusar()

    @ParameterizedTest
    @EnumSource(value = StatusPagamento.class, names = {"PENDENTE", "PROCESSANDO"})
    public void given_pagamentoEmAberto_when_recusa_then_statusRecusado(StatusPagamento status) {
        Pagamento pagamento = pagamentoComStatus(status);

        pagamento.recusar("mp-888", DEPOIS);

        assertEquals(StatusPagamento.RECUSADO, pagamento.getStatus());
        assertEquals("mp-888", pagamento.getIdTransacaoProvedor());
        assertEquals(DEPOIS, pagamento.getDataAtualizacao());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPagamento.class, names = {"APROVADO", "RECUSADO", "CANCELADO", "ESTORNADO"})
    public void given_pagamentoFinalizado_when_recusa_then_lancaConflict(StatusPagamento status) {
        Pagamento pagamento = pagamentoComStatus(status);

        assertThrows(ConflictException.class, () -> pagamento.recusar("mp-888", DEPOIS));
        assertEquals(status, pagamento.getStatus());
    }

    // cancelar()

    @ParameterizedTest
    @EnumSource(value = StatusPagamento.class, names = {"PENDENTE", "PROCESSANDO"})
    public void given_pagamentoEmAberto_when_cancela_then_statusCancelado(StatusPagamento status) {
        Pagamento pagamento = pagamentoComStatus(status);

        pagamento.cancelar(DEPOIS);

        assertEquals(StatusPagamento.CANCELADO, pagamento.getStatus());
        assertEquals(DEPOIS, pagamento.getDataAtualizacao());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPagamento.class, names = {"APROVADO", "RECUSADO", "CANCELADO", "ESTORNADO"})
    public void given_pagamentoFinalizado_when_cancela_then_lancaConflict(StatusPagamento status) {
        Pagamento pagamento = pagamentoComStatus(status);

        assertThrows(ConflictException.class, () -> pagamento.cancelar(DEPOIS));
        assertEquals(status, pagamento.getStatus());
    }

    // estornar()

    @Test
    public void given_pagamentoAprovado_when_estorna_then_statusEstornado() {
        Pagamento pagamento = pagamentoComStatus(StatusPagamento.APROVADO);

        pagamento.estornar(DEPOIS);

        assertEquals(StatusPagamento.ESTORNADO, pagamento.getStatus());
        assertEquals(DEPOIS, pagamento.getDataAtualizacao());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPagamento.class, names = "APROVADO", mode = EnumSource.Mode.EXCLUDE)
    public void given_pagamentoNaoAprovado_when_estorna_then_lancaConflict(StatusPagamento status) {
        Pagamento pagamento = pagamentoComStatus(status);

        assertThrows(ConflictException.class, () -> pagamento.estornar(DEPOIS));
        assertEquals(status, pagamento.getStatus());
    }
}
