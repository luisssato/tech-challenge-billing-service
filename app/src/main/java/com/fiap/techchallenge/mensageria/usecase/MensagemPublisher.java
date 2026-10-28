package com.fiap.techchallenge.mensageria.usecase;

import com.fiap.techchallenge.mensageria.domain.OutboxEvent;

public interface MensagemPublisher {

    void publicar(OutboxEvent evento);
}
