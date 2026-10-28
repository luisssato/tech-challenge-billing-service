package com.fiap.techchallenge.peca.adapter.controller;

import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.exception.NotFoundException;
import com.fiap.techchallenge.peca.domain.TipoPeca;
import com.fiap.techchallenge.peca.usecase.TipoPecaUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TipoPecaController.class)
@Import(TipoPecaMapper.class)
class TipoPecaControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean TipoPecaUseCase useCase;

    private final TipoPeca filtro = new TipoPeca(7L, "Filtro", new BigDecimal("30.00"));

    @Test
    void listaTiposPeca() throws Exception {
        when(useCase.getAllTiposPeca()).thenReturn(List.of(filtro));

        mvc.perform(get("/api/tipoPeca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].nome").value("Filtro"))
                .andExpect(jsonPath("$[0].valorUnitario").value(30.00));
    }

    @Test
    void buscaPorId() throws Exception {
        when(useCase.getTipoPeca(7L)).thenReturn(Optional.of(filtro));

        mvc.perform(get("/api/tipoPeca/7")).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Filtro"));
    }

    @Test
    void buscaPorIdInexistenteRetorna404() throws Exception {
        when(useCase.getTipoPeca(9L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/tipoPeca/9")).andExpect(status().isNotFound());
    }

    @Test
    void criaComIdOpcional() throws Exception {
        when(useCase.createTipoPeca(eq(7L), eq("Filtro"), any())).thenReturn(filtro);

        mvc.perform(post("/api/tipoPeca").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":7,\"nome\":\"Filtro\",\"valorUnitario\":30.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void criarSemNomeRetorna400() throws Exception {
        mvc.perform(post("/api/tipoPeca").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorUnitario\":30.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nome não pode ser nulo!"));
    }

    @Test
    void criarSemValorRetorna400() throws Exception {
        mvc.perform(post("/api/tipoPeca").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Filtro\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor Unitário não pode ser nulo!"));
    }

    @Test
    void criarComJsonMalformadoRetorna400() throws Exception {
        mvc.perform(post("/api/tipoPeca").contentType(MediaType.APPLICATION_JSON).content("{nome"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarComIdExistenteRetorna409() throws Exception {
        when(useCase.createTipoPeca(eq(7L), any(), any())).thenThrow(new ConflictException("Tipo de peça de id 7 já existe!"));

        mvc.perform(post("/api/tipoPeca").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":7,\"nome\":\"Filtro\",\"valorUnitario\":30.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void atualiza() throws Exception {
        when(useCase.updateTipoPeca(eq(7L), eq("Filtro"), any())).thenReturn(filtro);

        mvc.perform(put("/api/tipoPeca/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Filtro\",\"valorUnitario\":35.00}"))
                .andExpect(status().isOk());
    }

    @Test
    void atualizarInexistenteRetorna404() throws Exception {
        when(useCase.updateTipoPeca(eq(9L), any(), any())).thenThrow(new NotFoundException("não encontrado"));

        mvc.perform(put("/api/tipoPeca/9").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void remove() throws Exception {
        when(useCase.deleteTipoPeca(7L)).thenReturn(filtro);

        mvc.perform(delete("/api/tipoPeca/7")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
    }
}
