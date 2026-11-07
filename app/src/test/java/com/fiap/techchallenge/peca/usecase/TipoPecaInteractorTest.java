package com.fiap.techchallenge.peca.usecase;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.exception.NotFoundException;
import com.fiap.techchallenge.peca.domain.TipoPeca;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TipoPecaInteractorTest {

    private TipoPecaGateway gateway;
    private TipoPecaInteractor interactor;

    @BeforeEach
    void setUp() {
        gateway = mock(TipoPecaGateway.class);
        interactor = new TipoPecaInteractor(gateway);
        when(gateway.save(any(TipoPeca.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void listaEBuscaDelegamAoGateway() {
        TipoPeca peca = new TipoPeca(7L, "Filtro", new BigDecimal("30.00"));
        when(gateway.findAll()).thenReturn(List.of(peca));
        when(gateway.findById(7L)).thenReturn(Optional.of(peca));

        assertThat(interactor.getAllTiposPeca()).containsExactly(peca);
        assertThat(interactor.getTipoPeca(7L)).contains(peca);
    }

    @Test
    void criaComIdExplicitoQuandoLivre() {
        when(gateway.existsById(7L)).thenReturn(false);

        TipoPeca criada = interactor.createTipoPeca(7L, "Filtro", new BigDecimal("30.00"));

        assertThat(criada.getId()).isEqualTo(7L);
        verify(gateway, never()).proximoId();
    }

    @Test
    void recusaIdExplicitoJaExistente() {
        when(gateway.existsById(7L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> interactor.createTipoPeca(7L, "Filtro", new BigDecimal("30.00")));
        verify(gateway, never()).save(any());
    }

    @Test
    void recusaIdExplicitoNaoPositivo() {
        assertThrows(BadRequestException.class, () -> interactor.createTipoPeca(0L, "Filtro", new BigDecimal("30.00")));
    }

    @Test
    void geraIdPelaSequenciaPulandoIdsOcupados() {
        when(gateway.proximoId()).thenReturn(1L, 2L);
        when(gateway.existsById(1L)).thenReturn(true);
        when(gateway.existsById(2L)).thenReturn(false);

        TipoPeca criada = interactor.createTipoPeca(null, "Filtro", new BigDecimal("30.00"));

        assertThat(criada.getId()).isEqualTo(2L);
    }

    @Test
    void naoConsomeASequenciaQuandoOsDadosSaoInvalidos() {
        assertThrows(BadRequestException.class, () -> interactor.createTipoPeca(null, " ", new BigDecimal("30.00")));
        verify(gateway, never()).proximoId();
    }

    @Test
    void atualizaApenasOsCamposInformados() {
        when(gateway.findById(7L)).thenReturn(Optional.of(new TipoPeca(7L, "Filtro", new BigDecimal("30.00"))));

        TipoPeca atualizada = interactor.updateTipoPeca(7L, null, new BigDecimal("35.00"));

        assertThat(atualizada.getNome()).isEqualTo("Filtro");
        assertThat(atualizada.getValorUnitario()).isEqualByComparingTo("35.00");
    }

    @Test
    void atualizarInexistenteLancaNotFound() {
        when(gateway.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> interactor.updateTipoPeca(9L, "x", null));
    }

    @Test
    void removeERetornaOTipoRemovido() {
        TipoPeca peca = new TipoPeca(7L, "Filtro", new BigDecimal("30.00"));
        when(gateway.findById(7L)).thenReturn(Optional.of(peca));

        assertThat(interactor.deleteTipoPeca(7L)).isSameAs(peca);
        verify(gateway).deleteById(7L);
    }

    @Test
    void removerInexistenteLancaNotFound() {
        when(gateway.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> interactor.deleteTipoPeca(9L));
        verify(gateway, never()).deleteById(any());
    }
}
