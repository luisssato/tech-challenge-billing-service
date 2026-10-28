package com.fiap.techchallenge.mensageria.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class ProcessedEvent {

    private final UUID eventId;
    private final String type;
    private final String correlationId;
    private final LocalDateTime processedAt;

    public ProcessedEvent(UUID eventId, String type, String correlationId, LocalDateTime processedAt) {
        this.eventId = eventId;
        this.type = type;
        this.correlationId = correlationId;
        this.processedAt = processedAt;
    }

    public UUID getEventId() { return eventId; }
    public String getType() { return type; }
    public String getCorrelationId() { return correlationId; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
