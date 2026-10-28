package com.fiap.techchallenge.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    public void handleConflict_returns409ComMensagem() {
        var resposta = handler.handleConflict(new ConflictException("conflito"));

        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
        assertEquals(409, resposta.getBody().status());
        assertEquals("conflito", resposta.getBody().message());
    }

    @Test
    public void handleNotFound_returns404ComMensagem() {
        var resposta = handler.handleNotFound(new NotFoundException("não achei"));

        assertEquals(HttpStatus.NOT_FOUND, resposta.getStatusCode());
        assertEquals(404, resposta.getBody().status());
        assertEquals("não achei", resposta.getBody().message());
    }

    @Test
    public void handleBadRequest_returns400ComMensagem() {
        var resposta = handler.handleBadRequest(new BadRequestException("inválido"));

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals(400, resposta.getBody().status());
        assertEquals("inválido", resposta.getBody().message());
    }

    @Test
    public void handleValidation_returns400ComMensagensOrdenadas() {
        var binding = new org.springframework.validation.BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new org.springframework.validation.FieldError("request", "valor", "Valor não pode ser nulo!"));
        binding.addError(new org.springframework.validation.FieldError("request", "nome", "Nome não pode ser nulo!"));
        var ex = new org.springframework.web.bind.MethodArgumentNotValidException(null, binding);

        var resposta = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("Nome não pode ser nulo!; Valor não pode ser nulo!", resposta.getBody().message());
    }

    @Test
    public void handleValidation_semErrosDeCampoUsaMensagemPadrao() {
        var binding = new org.springframework.validation.BeanPropertyBindingResult(new Object(), "request");
        var ex = new org.springframework.web.bind.MethodArgumentNotValidException(null, binding);

        assertEquals("Requisição inválida!", handler.handleValidation(ex).getBody().message());
    }

    @Test
    public void handleUnreadable_returns400() {
        var resposta = handler.handleUnreadable(
                new org.springframework.http.converter.HttpMessageNotReadableException("x", (org.springframework.http.HttpInputMessage) null));

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("Corpo da requisição inválido ou malformado!", resposta.getBody().message());
    }
}
