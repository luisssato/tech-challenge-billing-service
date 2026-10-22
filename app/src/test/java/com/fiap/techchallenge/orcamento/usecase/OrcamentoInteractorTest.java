package com.fiap.techchallenge.orcamento.usecase;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.exception.NotFoundException;
import com.fiap.techchallenge.orcamento.domain.ItemOrcamento;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.domain.StatusOrcamento;
import com.fiap.techchallenge.orcamento.domain.TipoItem;
import com.fiap.techchallenge.orcamento.inputdata.ItemDiagnosticoInputData;
import com.fiap.techchallenge.peca.domain.TipoPeca;
import com.fiap.techchallenge.peca.usecase.TipoPecaGateway;
import com.fiap.techchallenge.servico.domain.TipoServico;
import com.fiap.techchallenge.servico.usecase.TipoServicoGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrcamentoInteractorTest {

    private static final Instant INSTANTE = Instant.parse("2026-10-21T13:00:00Z");
    private static final LocalDateTime AGORA = LocalDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);

    @Mock
    private OrcamentoGateway orcamentoGateway;

    @Mock
    private TipoPecaGateway tipoPecaGateway;

    @Mock
    private TipoServicoGateway tipoServicoGateway;

    private OrcamentoInteractor interactor;

    @BeforeEach
    public void setUp() {
        Clock clock = Clock.fixed(INSTANTE, ZoneOffset.UTC);
        interactor = new OrcamentoInteractor(orcamentoGateway, tipoPecaGateway, tipoServicoGateway, clock);
    }

    private Orcamento orcamentoExistente(StatusOrcamento status, LocalDateTime criadoEm) {
        var itens = List.of(
                new ItemOrcamento(TipoItem.PECA, 1L, "Pastilha", 2, new BigDecimal("50.00")),
                new ItemOrcamento(TipoItem.SERVICO, 2L, "Troca", 1, new BigDecimal("80.00")));
        return new Orcamento("orc-1", 10L, status, itens, criadoEm,
                criadoEm.plusDays(Orcamento.VALIDADE_EM_DIAS), criadoEm);
    }

    // getOrcamentoPorOs()

    @Test
    public void getOrcamentoPorOs_quandoExiste_returnsOrcamento() {
        var orcamento = orcamentoExistente(StatusOrcamento.GERADO, AGORA);
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.of(orcamento));

        var resultado = interactor.getOrcamentoPorOs(10L);

        assertTrue(resultado.isPresent());
        assertEquals("orc-1", resultado.get().getId());
    }

    @Test
    public void getOrcamentoPorOs_quandoNaoExiste_returnsVazio() {
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.empty());

        assertTrue(interactor.getOrcamentoPorOs(10L).isEmpty());
    }

    // gerarOrcamento()

    @Test
    public void gerarOrcamento_quandoDadosValidos_returnsOrcamentoSalvoComPrecosDoCatalogo() {
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.empty());
        when(tipoPecaGateway.findById(1L)).thenReturn(Optional.of(new TipoPeca(1L, "Pastilha", new BigDecimal("50.00"))));
        when(tipoServicoGateway.findById(2L)).thenReturn(Optional.of(new TipoServico(2L, "Troca", new BigDecimal("80.00"))));
        when(orcamentoGateway.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));

        var resultado = interactor.gerarOrcamento(10L,
                List.of(new ItemDiagnosticoInputData(1L, 2)),
                List.of(new ItemDiagnosticoInputData(2L, 1)));

        var captor = ArgumentCaptor.forClass(Orcamento.class);
        verify(orcamentoGateway).save(captor.capture());
        var salvo = captor.getValue();

        assertSame(salvo, resultado);
        assertEquals(10L, salvo.getOsId());
        assertEquals(StatusOrcamento.GERADO, salvo.getStatus());
        assertEquals(AGORA, salvo.getDataCriacao());
        assertEquals(new BigDecimal("180.00"), salvo.getValorTotal());
        assertEquals(2, salvo.getItens().size());
    }

    @Test
    public void gerarOrcamento_quandoJaExisteParaAOs_returnsExistenteSemSalvarNovamente() {
        var existente = orcamentoExistente(StatusOrcamento.GERADO, AGORA);
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.of(existente));

        var resultado = interactor.gerarOrcamento(10L,
                List.of(new ItemDiagnosticoInputData(1L, 2)),
                List.of(new ItemDiagnosticoInputData(2L, 1)));

        assertSame(existente, resultado);
        verify(orcamentoGateway, never()).save(any());
        verifyNoInteractions(tipoPecaGateway, tipoServicoGateway);
    }

    @Test
    public void gerarOrcamento_quandoOsIdNulo_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> interactor.gerarOrcamento(null, List.of(), List.of()));

        verifyNoInteractions(orcamentoGateway);
    }

    @Test
    public void gerarOrcamento_quandoTipoPecaNaoExiste_throwsNotFound() {
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.empty());
        when(tipoPecaGateway.findById(99L)).thenReturn(Optional.empty());

        var pecas = List.of(new ItemDiagnosticoInputData(99L, 1));
        var servicos = List.of(new ItemDiagnosticoInputData(2L, 1));

        assertThrows(NotFoundException.class, () -> interactor.gerarOrcamento(10L, pecas, servicos));

        verify(orcamentoGateway, never()).save(any());
    }

    @Test
    public void gerarOrcamento_quandoTipoServicoNaoExiste_throwsNotFound() {
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.empty());
        when(tipoPecaGateway.findById(1L)).thenReturn(Optional.of(new TipoPeca(1L, "Pastilha", new BigDecimal("50.00"))));
        when(tipoServicoGateway.findById(99L)).thenReturn(Optional.empty());

        var pecas = List.of(new ItemDiagnosticoInputData(1L, 1));
        var servicos = List.of(new ItemDiagnosticoInputData(99L, 1));

        assertThrows(NotFoundException.class, () -> interactor.gerarOrcamento(10L, pecas, servicos));

        verify(orcamentoGateway, never()).save(any());
    }

    @Test
    public void gerarOrcamento_quandoListasNulas_throwsBadRequest() {
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> interactor.gerarOrcamento(10L, null, null));

        verify(orcamentoGateway, never()).save(any());
    }

    @Test
    public void gerarOrcamento_quandoSemServicos_throwsBadRequest() {
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.empty());
        when(tipoPecaGateway.findById(1L)).thenReturn(Optional.of(new TipoPeca(1L, "Pastilha", new BigDecimal("50.00"))));

        var pecas = List.of(new ItemDiagnosticoInputData(1L, 1));

        assertThrows(BadRequestException.class, () -> interactor.gerarOrcamento(10L, pecas, List.of()));

        verify(orcamentoGateway, never()).save(any());
    }

    // aprovarOrcamento()

    @Test
    public void aprovarOrcamento_quandoGeradoEVigente_returnsAprovado() {
        var orcamento = orcamentoExistente(StatusOrcamento.GERADO, AGORA.minusDays(1));
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.of(orcamento));
        when(orcamentoGateway.save(orcamento)).thenReturn(orcamento);

        var resultado = interactor.aprovarOrcamento(10L);

        assertEquals(StatusOrcamento.APROVADO, resultado.getStatus());
        assertEquals(AGORA, resultado.getDataAtualizacao());
        verify(orcamentoGateway).save(orcamento);
    }

    @Test
    public void aprovarOrcamento_quandoNaoExiste_throwsNotFound() {
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> interactor.aprovarOrcamento(10L));

        verify(orcamentoGateway, never()).save(any());
    }

    @Test
    public void aprovarOrcamento_quandoVencido_persisteExpiradoEThrowsConflict() {
        var orcamento = orcamentoExistente(StatusOrcamento.GERADO, AGORA.minusDays(Orcamento.VALIDADE_EM_DIAS + 1));
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.of(orcamento));

        assertThrows(ConflictException.class, () -> interactor.aprovarOrcamento(10L));

        assertEquals(StatusOrcamento.EXPIRADO, orcamento.getStatus());
        verify(orcamentoGateway).save(orcamento);
    }

    @Test
    public void aprovarOrcamento_quandoJaAprovado_throwsConflictSemSalvar() {
        var orcamento = orcamentoExistente(StatusOrcamento.APROVADO, AGORA.minusDays(1));
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.of(orcamento));

        assertThrows(ConflictException.class, () -> interactor.aprovarOrcamento(10L));

        verify(orcamentoGateway, never()).save(any());
    }

    // rejeitarOrcamento()

    @Test
    public void rejeitarOrcamento_quandoGerado_returnsRejeitado() {
        var orcamento = orcamentoExistente(StatusOrcamento.GERADO, AGORA.minusDays(1));
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.of(orcamento));
        when(orcamentoGateway.save(orcamento)).thenReturn(orcamento);

        var resultado = interactor.rejeitarOrcamento(10L);

        assertEquals(StatusOrcamento.REJEITADO, resultado.getStatus());
        assertEquals(AGORA, resultado.getDataAtualizacao());
        verify(orcamentoGateway).save(orcamento);
    }

    @Test
    public void rejeitarOrcamento_quandoNaoExiste_throwsNotFound() {
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> interactor.rejeitarOrcamento(10L));

        verify(orcamentoGateway, never()).save(any());
    }

    @Test
    public void rejeitarOrcamento_quandoJaAprovado_throwsConflictSemSalvar() {
        var orcamento = orcamentoExistente(StatusOrcamento.APROVADO, AGORA.minusDays(1));
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.of(orcamento));

        assertThrows(ConflictException.class, () -> interactor.rejeitarOrcamento(10L));

        verify(orcamentoGateway, never()).save(any());
    }
}
