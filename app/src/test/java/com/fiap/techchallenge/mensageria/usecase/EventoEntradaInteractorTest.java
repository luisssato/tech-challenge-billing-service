package com.fiap.techchallenge.mensageria.usecase;

import com.fiap.techchallenge.mensageria.domain.TipoEvento;
import com.fiap.techchallenge.orcamento.domain.ItemOrcamento;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.domain.StatusOrcamento;
import com.fiap.techchallenge.orcamento.domain.TipoItem;
import com.fiap.techchallenge.orcamento.inputdata.ItemDiagnosticoInputData;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoGateway;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoUseCase;
import com.fiap.techchallenge.pagamento.domain.Pagamento;
import com.fiap.techchallenge.pagamento.domain.StatusPagamento;
import com.fiap.techchallenge.pagamento.usecase.PagamentoGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventoEntradaInteractorTest {

    private final OrcamentoUseCase orcamentoUseCase = mock(OrcamentoUseCase.class);
    private final OrcamentoGateway orcamentoGateway = mock(OrcamentoGateway.class);
    private final PagamentoGateway pagamentoGateway = mock(PagamentoGateway.class);
    private final ProcessedEventGateway processedEventGateway = mock(ProcessedEventGateway.class);
    private final EventoSaidaGateway eventoSaidaGateway = mock(EventoSaidaGateway.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneId.of("UTC"));

    private EventoEntradaInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new EventoEntradaInteractor(
                orcamentoUseCase, orcamentoGateway, pagamentoGateway,
                processedEventGateway, eventoSaidaGateway, clock);
    }

    @Test
    void processarDiagnosticoConcluidoGeraOrcamentoEPublicaEvento() {
        UUID eventId = UUID.randomUUID();
        Long osId = 10L;
        List<ItemDiagnosticoInputData> pecas = List.of(new ItemDiagnosticoInputData(1L, 2));
        List<ItemDiagnosticoInputData> servicos = List.of(new ItemDiagnosticoInputData(2L, 1));

        when(processedEventGateway.jaProcessado(eventId)).thenReturn(false);

        LocalDateTime agora = LocalDateTime.now(clock);
        Orcamento orcamento = new Orcamento("orc-1", osId, StatusOrcamento.GERADO, List.of(
                new ItemOrcamento(TipoItem.PECA, 1L, "Pastilha", 2, new BigDecimal("50.00")),
                new ItemOrcamento(TipoItem.SERVICO, 2L, "Troca", 1, new BigDecimal("80.00"))),
                agora, agora.plusDays(5), agora);
        when(orcamentoUseCase.gerarOrcamento(osId, pecas, servicos)).thenReturn(orcamento);

        interactor.processarDiagnosticoConcluido(eventId, osId, pecas, servicos);

        verify(orcamentoUseCase).gerarOrcamento(osId, pecas, servicos);
        verify(eventoSaidaGateway).publicarEvento(eq(TipoEvento.ORCAMENTO_GERADO), eq("10"), any());
        verify(processedEventGateway).registrarProcessado(eventId, TipoEvento.DIAGNOSTICO_CONCLUIDO, "10");
    }

    @Test
    void eventoJaProcessadoIgnoraDuplicidadeIdempotente() {
        UUID eventId = UUID.randomUUID();
        when(processedEventGateway.jaProcessado(eventId)).thenReturn(true);

        interactor.processarDiagnosticoConcluido(eventId, 10L, List.of(), List.of());

        verify(orcamentoUseCase, never()).gerarOrcamento(any(), any(), any());
        verify(eventoSaidaGateway, never()).publicarEvento(any(), any(), any());
    }

    @Test
    void processarExecucaoFalhouEstornaPagamentoAprovado() {
        UUID eventId = UUID.randomUUID();
        Long osId = 20L;
        when(processedEventGateway.jaProcessado(eventId)).thenReturn(false);

        LocalDateTime agora = LocalDateTime.now(clock);
        Pagamento aprovado = Pagamento.criar(osId, "orc-2", new BigDecimal("300.00"), agora);
        aprovado.confirmar("mp-999", agora);
        when(pagamentoGateway.findUltimoByOsId(osId)).thenReturn(Optional.of(aprovado));

        interactor.processarExecucaoFalhou(eventId, osId, "SEM_ESTOQUE", "Peça indisponível");

        verify(pagamentoGateway).save(aprovado);
        verify(eventoSaidaGateway).publicarEvento(eq(TipoEvento.PAGAMENTO_ESTORNADO), eq("20"), any());
        verify(processedEventGateway).registrarProcessado(eventId, TipoEvento.EXECUCAO_FALHOU, "20");
    }

    @Test
    void processarOsCanceladaRejeitaOrcamentoECancelaPagamentoPendente() {
        UUID eventId = UUID.randomUUID();
        Long osId = 30L;
        when(processedEventGateway.jaProcessado(eventId)).thenReturn(false);

        LocalDateTime agora = LocalDateTime.now(clock);
        Orcamento orcamento = new Orcamento("orc-3", osId, StatusOrcamento.GERADO, List.of(
                new ItemOrcamento(TipoItem.PECA, 1L, "P1", 1, new BigDecimal("10.00")),
                new ItemOrcamento(TipoItem.SERVICO, 2L, "S1", 1, new BigDecimal("20.00"))),
                agora, agora.plusDays(5), agora);
        when(orcamentoGateway.findByOsId(osId)).thenReturn(Optional.of(orcamento));

        Pagamento pendente = Pagamento.criar(osId, "orc-3", new BigDecimal("30.00"), agora);
        when(pagamentoGateway.findUltimoByOsId(osId)).thenReturn(Optional.of(pendente));

        interactor.processarOsCancelada(eventId, osId, "CLIENTE_DESISTIU");

        verify(orcamentoGateway).save(orcamento);
        verify(pagamentoGateway).save(pendente);
        verify(processedEventGateway).registrarProcessado(eventId, TipoEvento.OS_CANCELADA, "30");
    }
}
