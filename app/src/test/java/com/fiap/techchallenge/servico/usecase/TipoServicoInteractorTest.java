package com.fiap.techchallenge.servico.usecase;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.exception.NotFoundException;
import com.fiap.techchallenge.servico.domain.TipoServico;
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

class TipoServicoInteractorTest {

    private TipoServicoGateway gateway;
    private TipoServicoInteractor interactor;

    @BeforeEach
    void setUp() {
        gateway = mock(TipoServicoGateway.class);
        interactor = new TipoServicoInteractor(gateway);
        when(gateway.save(any(TipoServico.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void listaEBuscaDelegamAoGateway() {
        TipoServico peca = new TipoServico(7L, "Troca de óleo", new BigDecimal("30.00"));
        when(gateway.findAll()).thenReturn(List.of(peca));
        when(gateway.findById(7L)).thenReturn(Optional.of(peca));

        assertThat(interactor.getAllTiposServico()).containsExactly(peca);
        assertThat(interactor.getTipoServico(7L)).contains(peca);
    }

    @Test
    void criaComIdExplicitoQuandoLivre() {
        when(gateway.existsById(7L)).thenReturn(false);

        TipoServico criada = interactor.createTipoServico(7L, "Troca de óleo", new BigDecimal("30.00"));

        assertThat(criada.getId()).isEqualTo(7L);
        verify(gateway, never()).proximoId();
    }

    @Test
    void recusaIdExplicitoJaExistente() {
        when(gateway.existsById(7L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> interactor.createTipoServico(7L, "Troca de óleo", new BigDecimal("30.00")));
        verify(gateway, never()).save(any());
    }

    @Test
    void recusaIdExplicitoNaoPositivo() {
        assertThrows(BadRequestException.class, () -> interactor.createTipoServico(0L, "Troca de óleo", new BigDecimal("30.00")));
    }

    @Test
    void geraIdPelaSequenciaPulandoIdsOcupados() {
        when(gateway.proximoId()).thenReturn(1L, 2L);
        when(gateway.existsById(1L)).thenReturn(true);
        when(gateway.existsById(2L)).thenReturn(false);

        TipoServico criada = interactor.createTipoServico(null, "Troca de óleo", new BigDecimal("30.00"));

        assertThat(criada.getId()).isEqualTo(2L);
    }

    @Test
    void naoConsomeASequenciaQuandoOsDadosSaoInvalidos() {
        assertThrows(BadRequestException.class, () -> interactor.createTipoServico(null, " ", new BigDecimal("30.00")));
        verify(gateway, never()).proximoId();
    }

    @Test
    void atualizaApenasOsCamposInformados() {
        when(gateway.findById(7L)).thenReturn(Optional.of(new TipoServico(7L, "Troca de óleo", new BigDecimal("30.00"))));

        TipoServico atualizada = interactor.updateTipoServico(7L, null, new BigDecimal("35.00"));

        assertThat(atualizada.getNome()).isEqualTo("Troca de óleo");
        assertThat(atualizada.getValor()).isEqualByComparingTo("35.00");
    }

    @Test
    void atualizarInexistenteLancaNotFound() {
        when(gateway.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> interactor.updateTipoServico(9L, "x", null));
    }

    @Test
    void removeERetornaOTipoRemovido() {
        TipoServico peca = new TipoServico(7L, "Troca de óleo", new BigDecimal("30.00"));
        when(gateway.findById(7L)).thenReturn(Optional.of(peca));

        assertThat(interactor.deleteTipoServico(7L)).isSameAs(peca);
        verify(gateway).deleteById(7L);
    }

    @Test
    void removerInexistenteLancaNotFound() {
        when(gateway.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> interactor.deleteTipoServico(9L));
        verify(gateway, never()).deleteById(any());
    }
}
