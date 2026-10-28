package com.fiap.techchallenge.mensageria.usecase;

import com.fiap.techchallenge.mensageria.domain.OutboxEvent;

import java.util.List;

public interface OutboxEventGateway {

    OutboxEvent salvar(OutboxEvent evento);

    List<OutboxEvent> buscarPendentes(int limite);
}
