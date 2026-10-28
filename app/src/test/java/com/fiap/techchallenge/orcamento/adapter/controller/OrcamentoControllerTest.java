package com.fiap.techchallenge.orcamento.adapter.controller;

import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.exception.NotFoundException;
import com.fiap.techchallenge.orcamento.domain.ItemOrcamento;
import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.domain.StatusOrcamento;
import com.fiap.techchallenge.orcamento.domain.TipoItem;
import com.fiap.techchallenge.orcamento.usecase.OrcamentoUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrcamentoController.class)
@Import(OrcamentoMapper.class)
class OrcamentoControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean OrcamentoUseCase useCase;

    private Orcamento orcamento(StatusOrcamento status) {
        LocalDateTime agora = LocalDateTime.of(2026, 10, 28, 10, 0);
        return new Orcamento("abc123", 10L, status, List.of(
                new ItemOrcamento(TipoItem.PECA, 7L, "Pastilha", 2, new BigDecimal("150.00")),
                new ItemOrcamento(TipoItem.SERVICO, 3L, "Troca", 1, new BigDecimal("100.00"))),
                agora, agora.plusDays(5), agora);
    }

    @Test
    void consultaOrcamentoComItensETotais() throws Exception {
        when(useCase.getOrcamentoPorOs(10L)).thenReturn(Optional.of(orcamento(StatusOrcamento.GERADO)));

        mvc.perform(get("/api/orcamento/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc123"))
                .andExpect(jsonPath("$.osId").value(10))
                .andExpect(jsonPath("$.status").value("GERADO"))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].tipo").value("PECA"))
                .andExpect(jsonPath("$.itens[0].subtotal").value(300.00))
                .andExpect(jsonPath("$.valorTotalPecas").value(300.00))
                .andExpect(jsonPath("$.valorTotalServicos").value(100.00))
                .andExpect(jsonPath("$.valorTotal").value(400.00));
    }

    @Test
    void consultaInexistenteRetorna404() throws Exception {
        when(useCase.getOrcamentoPorOs(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/orcamento/99")).andExpect(status().isNotFound());
    }

    @Test
    void aprova() throws Exception {
        when(useCase.aprovarOrcamento(10L)).thenReturn(orcamento(StatusOrcamento.APROVADO));

        mvc.perform(post("/api/orcamento/10/aprovar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    void aprovarOrcamentoExpiradoRetorna409() throws Exception {
        when(useCase.aprovarOrcamento(10L)).thenThrow(new ConflictException("expirou"));

        mvc.perform(post("/api/orcamento/10/aprovar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("expirou"));
    }

    @Test
    void rejeita() throws Exception {
        when(useCase.rejeitarOrcamento(10L)).thenReturn(orcamento(StatusOrcamento.REJEITADO));

        mvc.perform(post("/api/orcamento/10/rejeitar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADO"));
    }

    @Test
    void rejeitarInexistenteRetorna404() throws Exception {
        when(useCase.rejeitarOrcamento(99L)).thenThrow(new NotFoundException("não encontrado"));

        mvc.perform(post("/api/orcamento/99/rejeitar")).andExpect(status().isNotFound());
    }
}
