package com.fiap.techchallenge.mensageria.usecase;

import java.util.UUID;

public interface ProcessedEventGateway {

    boolean jaProcessado(UUID eventId);

    void registrarProcessado(UUID eventId, String type, String correlationId);
}
