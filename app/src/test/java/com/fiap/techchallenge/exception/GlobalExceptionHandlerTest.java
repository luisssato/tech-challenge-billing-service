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
}
