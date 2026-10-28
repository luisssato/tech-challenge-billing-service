package com.fiap.techchallenge.pagamento.usecase;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.exception.NotFoundException;
import com.fiap.techchallenge.orcamento.domain.ItemOrcamento;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.domain.StatusOrcamento;
import com.fiap.techchallenge.orcamento.domain.TipoItem;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoGateway;
import com.fiap.techchallenge.pagamento.domain.Pagamento;
import com.fiap.techchallenge.pagamento.domain.StatusPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PagamentoInteractorTest {

    private final PagamentoGateway pagamentoGateway = mock(PagamentoGateway.class);
    private final OrcamentoGateway orcamentoGateway = mock(OrcamentoGateway.class);
    private final ProvedorPagamentoGateway provedorGateway = mock(ProvedorPagamentoGateway.class);
    private final com.fiap.techchallenge.mensageria.usecase.EventoSaidaGateway eventoSaidaGateway = mock(com.fiap.techchallenge.mensageria.usecase.EventoSaidaGateway.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneId.of("UTC"));

    private PagamentoInteractor interactor;

    private Orcamento orcamentoAprovado() {
        LocalDateTime agora = LocalDateTime.now(clock);
        Orcamento o = new Orcamento("orc-1", 10L, StatusOrcamento.GERADO, List.of(
                new ItemOrcamento(TipoItem.PECA, 1L, "Peça", 1, new BigDecimal("100.00")),
                new ItemOrcamento(TipoItem.SERVICO, 2L, "Serviço", 1, new BigDecimal("50.00"))),
                agora, agora.plusDays(5), agora);
        o.aprovar(agora);
        return o;
    }

    @BeforeEach
    void setUp() {
        interactor = new PagamentoInteractor(pagamentoGateway, orcamentoGateway, provedorGateway, eventoSaidaGateway, clock);
        when(pagamentoGateway.save(any(Pagamento.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void devolveOPagamentoMaisRecenteDaOs() {
        Pagamento pagamento = Pagamento.criar(10L, "orc-1", new BigDecimal("150.00"), LocalDateTime.now(clock));
        when(pagamentoGateway.findUltimoByOsId(10L)).thenReturn(Optional.of(pagamento));

        assertThat(interactor.getPagamentoPorOs(10L)).contains(pagamento);
    }

    @Test
    void osSemPagamentoRetornaVazio() {
        when(pagamentoGateway.findUltimoByOsId(99L)).thenReturn(Optional.empty());

        assertThat(interactor.getPagamentoPorOs(99L)).isEmpty();
    }

    @Test
    void iniciarPagamentoComSucessoGeraQrCode() {
        Orcamento orcamento = orcamentoAprovado();
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.of(orcamento));
        when(pagamentoGateway.findUltimoByOsId(10L)).thenReturn(Optional.empty());
        when(provedorGateway.criarCobranca(eq(10L), any(), any(), any()))
                .thenReturn(new CobrancaQrCode("mp-100", "000201...", "base64img"));

        Pagamento pagamento = interactor.iniciarPagamento(10L);

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(pagamento.getIdTransacaoProvedor()).isEqualTo("mp-100");
        assertThat(pagamento.getQrCode()).isEqualTo("000201...");
        assertThat(pagamento.getQrCodeBase64()).isEqualTo("base64img");
        verify(pagamentoGateway).save(pagamento);
    }

    @Test
    void iniciarPagamentoComOsIdNuloLancaBadRequestException() {
        assertThrows(BadRequestException.class, () -> interactor.iniciarPagamento(null));
    }

    @Test
    void iniciarPagamentoSemOrcamentoLancaNotFoundException() {
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> interactor.iniciarPagamento(10L));
    }

    @Test
    void iniciarPagamentoComOrcamentoNaoAprovadoLancaConflictException() {
        LocalDateTime agora = LocalDateTime.now(clock);
        Orcamento orcamentoGerado = Orcamento.gerar(10L, List.of(
                new ItemOrcamento(TipoItem.PECA, 1L, "Peça", 1, new BigDecimal("100.00")),
                new ItemOrcamento(TipoItem.SERVICO, 2L, "Serviço", 1, new BigDecimal("50.00"))), agora);
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.of(orcamentoGerado));

        assertThrows(ConflictException.class, () -> interactor.iniciarPagamento(10L));
    }

    @Test
    void iniciarPagamentoJaAprovadoLancaConflictException() {
        Orcamento orcamento = orcamentoAprovado();
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.of(orcamento));

        Pagamento aprovado = Pagamento.criar(10L, "orc-1", new BigDecimal("150.00"), LocalDateTime.now(clock));
        aprovado.confirmar("mp-pago", LocalDateTime.now(clock));
        when(pagamentoGateway.findUltimoByOsId(10L)).thenReturn(Optional.of(aprovado));

        assertThrows(ConflictException.class, () -> interactor.iniciarPagamento(10L));
    }

    @Test
    void iniciarPagamentoReaproveitaCobrancaPendenteExistenteIdempotente() {
        Orcamento orcamento = orcamentoAprovado();
        when(orcamentoGateway.findByOsId(10L)).thenReturn(Optional.of(orcamento));

        Pagamento pendente = Pagamento.criar(10L, "orc-1", new BigDecimal("150.00"), LocalDateTime.now(clock));
        pendente.registrarCobranca("mp-ja-existe", "qr-code", "qr-base64", LocalDateTime.now(clock));
        when(pagamentoGateway.findUltimoByOsId(10L)).thenReturn(Optional.of(pendente));

        Pagamento resultado = interactor.iniciarPagamento(10L);

        assertThat(resultado).isSameAs(pendente);
        verify(provedorGateway, never()).criarCobranca(any(), any(), any(), any());
    }

    @Test
    void processarWebhookComIdTransacaoInvalidoLancaBadRequest() {
        assertThrows(BadRequestException.class, () -> interactor.processarNotificacaoWebhook(null));
        assertThrows(BadRequestException.class, () -> interactor.processarNotificacaoWebhook(" "));
    }

    @Test
    void processarWebhookTransacaoNaoEncontradaRetornaVazio() {
        when(pagamentoGateway.findByIdTransacaoProvedor("mp-nao-existe")).thenReturn(Optional.empty());

        assertThat(interactor.processarNotificacaoWebhook("mp-nao-existe")).isEmpty();
    }

    @Test
    void processarWebhookJaAprovadoRetornaSemAlterarIdempotente() {
        Pagamento aprovado = Pagamento.criar(10L, "orc-1", new BigDecimal("150.00"), LocalDateTime.now(clock));
        aprovado.confirmar("mp-100", LocalDateTime.now(clock));
        when(pagamentoGateway.findByIdTransacaoProvedor("mp-100")).thenReturn(Optional.of(aprovado));

        Optional<Pagamento> resultado = interactor.processarNotificacaoWebhook("mp-100");

        assertThat(resultado).contains(aprovado);
        verify(provedorGateway, never()).consultarPagamento(any());
    }

    @Test
    void processarWebhookAprovadoNoProvedorConfirmaPagamento() {
        Pagamento pendente = Pagamento.criar(10L, "orc-1", new BigDecimal("150.00"), LocalDateTime.now(clock));
        pendente.registrarCobranca("mp-100", "qr", "qr-base64", LocalDateTime.now(clock));
        when(pagamentoGateway.findByIdTransacaoProvedor("mp-100")).thenReturn(Optional.of(pendente));
        when(provedorGateway.consultarPagamento("mp-100"))
                .thenReturn(new ConsultaPagamento("mp-100", StatusPagamentoProvedor.APROVADO, new BigDecimal("150.00")));

        Optional<Pagamento> resultado = interactor.processarNotificacaoWebhook("mp-100");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getStatus()).isEqualTo(StatusPagamento.APROVADO);
        verify(pagamentoGateway).save(pendente);
    }

    @Test
    void processarWebhookRecusadoNoProvedorRecusaPagamento() {
        Pagamento pendente = Pagamento.criar(10L, "orc-1", new BigDecimal("150.00"), LocalDateTime.now(clock));
        pendente.registrarCobranca("mp-101", "qr", "qr-base64", LocalDateTime.now(clock));
        when(pagamentoGateway.findByIdTransacaoProvedor("mp-101")).thenReturn(Optional.of(pendente));
        when(provedorGateway.consultarPagamento("mp-101"))
                .thenReturn(new ConsultaPagamento("mp-101", StatusPagamentoProvedor.RECUSADO, new BigDecimal("150.00")));

        Optional<Pagamento> resultado = interactor.processarNotificacaoWebhook("mp-101");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getStatus()).isEqualTo(StatusPagamento.RECUSADO);
    }

    @Test
    void processarWebhookCanceladoNoProvedorCancelaPagamento() {
        Pagamento pendente = Pagamento.criar(10L, "orc-1", new BigDecimal("150.00"), LocalDateTime.now(clock));
        pendente.registrarCobranca("mp-102", "qr", "qr-base64", LocalDateTime.now(clock));
        when(pagamentoGateway.findByIdTransacaoProvedor("mp-102")).thenReturn(Optional.of(pendente));
        when(provedorGateway.consultarPagamento("mp-102"))
                .thenReturn(new ConsultaPagamento("mp-102", StatusPagamentoProvedor.CANCELADO, new BigDecimal("150.00")));

        Optional<Pagamento> resultado = interactor.processarNotificacaoWebhook("mp-102");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getStatus()).isEqualTo(StatusPagamento.CANCELADO);
    }

    @Test
    void processarWebhookPendenteMantemStatus() {
        Pagamento pendente = Pagamento.criar(10L, "orc-1", new BigDecimal("150.00"), LocalDateTime.now(clock));
        pendente.registrarCobranca("mp-103", "qr", "qr-base64", LocalDateTime.now(clock));
        when(pagamentoGateway.findByIdTransacaoProvedor("mp-103")).thenReturn(Optional.of(pendente));
        when(provedorGateway.consultarPagamento("mp-103"))
                .thenReturn(new ConsultaPagamento("mp-103", StatusPagamentoProvedor.PENDENTE, new BigDecimal("150.00")));

        Optional<Pagamento> resultado = interactor.processarNotificacaoWebhook("mp-103");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getStatus()).isEqualTo(StatusPagamento.PENDENTE);
    }
}
