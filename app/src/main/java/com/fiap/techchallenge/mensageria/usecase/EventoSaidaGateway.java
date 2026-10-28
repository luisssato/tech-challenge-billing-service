package com.fiap.techchallenge.mensageria.usecase;

public interface EventoSaidaGateway {

    void publicarEvento(String tipo, String correlationId, Object payload);
}
