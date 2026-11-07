package com.fiap.techchallenge.servico.adapter.controller;

import com.fiap.techchallenge.exception.ConflictException;
import com.fiap.techchallenge.exception.NotFoundException;
import com.fiap.techchallenge.servico.domain.TipoServico;
import com.fiap.techchallenge.servico.usecase.TipoServicoUseCase;
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

@WebMvcTest(TipoServicoController.class)
@Import(TipoServicoMapper.class)
class TipoServicoControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean TipoServicoUseCase useCase;

    private final TipoServico filtro = new TipoServico(7L, "Troca de óleo", new BigDecimal("30.00"));

    @Test
    void listaTiposServico() throws Exception {
        when(useCase.getAllTiposServico()).thenReturn(List.of(filtro));

        mvc.perform(get("/api/tipo-servico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].nome").value("Troca de óleo"))
                .andExpect(jsonPath("$[0].valor").value(30.00));
    }

    @Test
    void buscaPorId() throws Exception {
        when(useCase.getTipoServico(7L)).thenReturn(Optional.of(filtro));

        mvc.perform(get("/api/tipo-servico/7")).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Troca de óleo"));
    }

    @Test
    void buscaPorIdInexistenteRetorna404() throws Exception {
        when(useCase.getTipoServico(9L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/tipo-servico/9")).andExpect(status().isNotFound());
    }

    @Test
    void criaComIdOpcional() throws Exception {
        when(useCase.createTipoServico(eq(7L), eq("Troca de óleo"), any())).thenReturn(filtro);

        mvc.perform(post("/api/tipo-servico").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":7,\"nome\":\"Troca de óleo\",\"valor\":30.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void criarSemNomeRetorna400() throws Exception {
        mvc.perform(post("/api/tipo-servico").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":30.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nome não pode ser nulo!"));
    }

    @Test
    void criarSemValorRetorna400() throws Exception {
        mvc.perform(post("/api/tipo-servico").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Troca de óleo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor não pode ser nulo!"));
    }

    @Test
    void criarComJsonMalformadoRetorna400() throws Exception {
        mvc.perform(post("/api/tipo-servico").contentType(MediaType.APPLICATION_JSON).content("{nome"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarComIdExistenteRetorna409() throws Exception {
        when(useCase.createTipoServico(eq(7L), any(), any())).thenThrow(new ConflictException("Tipo de serviço de id 7 já existe!"));

        mvc.perform(post("/api/tipo-servico").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":7,\"nome\":\"Troca de óleo\",\"valor\":30.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void atualiza() throws Exception {
        when(useCase.updateTipoServico(eq(7L), eq("Troca de óleo"), any())).thenReturn(filtro);

        mvc.perform(put("/api/tipo-servico/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Troca de óleo\",\"valor\":35.00}"))
                .andExpect(status().isOk());
    }

    @Test
    void atualizarInexistenteRetorna404() throws Exception {
        when(useCase.updateTipoServico(eq(9L), any(), any())).thenThrow(new NotFoundException("não encontrado"));

        mvc.perform(put("/api/tipo-servico/9").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void remove() throws Exception {
        when(useCase.deleteTipoServico(7L)).thenReturn(filtro);

        mvc.perform(delete("/api/tipo-servico/7")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
    }
}
