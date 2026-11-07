package com.fiap.techchallenge.pagamento.adapter.controller;

import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.exception.NotFoundException;
import com.fiap.techchallenge.pagamento.domain.Pagamento;
import com.fiap.techchallenge.pagamento.domain.StatusPagamento;
import com.fiap.techchallenge.pagamento.usecase.PagamentoUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PagamentoController.class)
@Import(PagamentoMapper.class)
class PagamentoControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean PagamentoUseCase useCase;

    private final LocalDateTime agora = LocalDateTime.of(2026, 10, 28, 10, 0);

    private Pagamento pagamento(StatusPagamento status) {
        return new Pagamento("p1", 10L, "orc-1", new BigDecimal("400.00"), status,
                "mp-123", "000201", "iVBOR", agora, agora);
    }

    @Test
    void consultaPagamentoDaOs() throws Exception {
        when(useCase.getPagamentoPorOs(10L)).thenReturn(Optional.of(pagamento(StatusPagamento.APROVADO)));

        mvc.perform(get("/api/pagamento/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.osId").value(10))
                .andExpect(jsonPath("$.orcamentoId").value("orc-1"))
                .andExpect(jsonPath("$.valor").value(400.00))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.idTransacaoProvedor").value("mp-123"))
                .andExpect(jsonPath("$.qrCode").value("000201"));
    }

    @Test
    void osSemPagamentoRetorna404() throws Exception {
        when(useCase.getPagamentoPorOs(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/pagamento/99")).andExpect(status().isNotFound());
    }

    @Test
    void iniciarPagamentoComSucessoRetorna201() throws Exception {
        when(useCase.iniciarPagamento(10L)).thenReturn(pagamento(StatusPagamento.PENDENTE));

        mvc.perform(post("/api/pagamento/10/iniciar"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.osId").value(10))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.qrCode").value("000201"));
    }

    @Test
    void iniciarPagamentoOrcamentoNaoEncontradoRetorna404() throws Exception {
        when(useCase.iniciarPagamento(99L)).thenThrow(new NotFoundException("Orçamento não encontrado"));

        mvc.perform(post("/api/pagamento/99/iniciar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void iniciarPagamentoOrcamentoNaoAprovadoRetorna409() throws Exception {
        when(useCase.iniciarPagamento(10L)).thenThrow(new ConflictException("Orçamento não aprovado"));

        mvc.perform(post("/api/pagamento/10/iniciar"))
                .andExpect(status().isConflict());
    }

    @Test
    void webhookComQueryParamDataIdRetorna200() throws Exception {
        when(useCase.processarNotificacaoWebhook("987654")).thenReturn(Optional.of(pagamento(StatusPagamento.APROVADO)));

        mvc.perform(post("/api/pagamento/webhook").param("data.id", "987654"))
                .andExpect(status().isOk());

        verify(useCase).processarNotificacaoWebhook("987654");
    }

    @Test
    void webhookComQueryParamIdRetorna200() throws Exception {
        when(useCase.processarNotificacaoWebhook("123456")).thenReturn(Optional.of(pagamento(StatusPagamento.APROVADO)));

        mvc.perform(post("/api/pagamento/webhook").param("id", "123456"))
                .andExpect(status().isOk());

        verify(useCase).processarNotificacaoWebhook("123456");
    }

    @Test
    void webhookComJsonBodyRetorna200() throws Exception {
        when(useCase.processarNotificacaoWebhook("555444")).thenReturn(Optional.of(pagamento(StatusPagamento.APROVADO)));

        String json = """
                {
                  "action": "payment.updated",
                  "data": { "id": "555444" }
                }
                """;

        mvc.perform(post("/api/pagamento/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(useCase).processarNotificacaoWebhook("555444");
    }

    @Test
    void webhookSemIdentificadorRetorna400() throws Exception {
        mvc.perform(post("/api/pagamento/webhook"))
                .andExpect(status().isBadRequest());
    }
}
