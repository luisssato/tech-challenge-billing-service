package com.fiap.techchallenge.mensageria.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class OutboxEvent {

    private final UUID id;
    private final String aggregateId;
    private final String type;
    private final String payload;
    private final LocalDateTime createdAt;
    private StatusOutbox status;
    private LocalDateTime sentAt;

    public OutboxEvent(UUID id, String aggregateId, String type, String payload,
                       LocalDateTime createdAt, StatusOutbox status, LocalDateTime sentAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.status = status != null ? status : StatusOutbox.PENDENTE;
        this.sentAt = sentAt;
    }

    public static OutboxEvent criar(String aggregateId, String type, String payload, LocalDateTime agora) {
        return new OutboxEvent(UUID.randomUUID(), aggregateId, type, payload, agora, StatusOutbox.PENDENTE, null);
    }

    public void marcarComoEnviado(LocalDateTime agora) {
        this.status = StatusOutbox.ENVIADO;
        this.sentAt = agora;
    }

    public void marcarComoFalha() {
        this.status = StatusOutbox.FALHA;
    }

    public UUID getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public StatusOutbox getStatus() { return status; }
    public LocalDateTime getSentAt() { return sentAt; }
}
