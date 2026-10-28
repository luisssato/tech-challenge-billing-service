package com.fiap.techchallenge.mensageria.adapter.gateway;

import com.fiap.techchallenge.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.mensageria.domain.StatusOutbox;
import com.fiap.techchallenge.mensageria.usecase.OutboxEventGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OutboxEventGatewayImpl implements OutboxEventGateway {

    static final String PK = "OUTBOX";
    static final String SK_PREFIX = "OUTBOX#";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public OutboxEventGatewayImpl(DynamoDbClient dynamoDbClient,
                                  @Value("${dynamodb.table.name:tech-challenge-prod-billing-service-table}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public OutboxEvent salvar(OutboxEvent evento) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("osId", AttributeValue.fromS(PK));
        item.put("sk", AttributeValue.fromS(SK_PREFIX + evento.getId().toString()));
        item.put("id", AttributeValue.fromS(evento.getId().toString()));
        item.put("aggregateId", AttributeValue.fromS(evento.getAggregateId()));
        item.put("type", AttributeValue.fromS(evento.getType()));
        item.put("payload", AttributeValue.fromS(evento.getPayload()));
        item.put("outboxStatus", AttributeValue.fromS(evento.getStatus().name()));
        item.put("createdAt", AttributeValue.fromS(evento.getCreatedAt().toString()));
        if (evento.getSentAt() != null) {
            item.put("sentAt", AttributeValue.fromS(evento.getSentAt().toString()));
        }

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();
        dynamoDbClient.putItem(request);
        return evento;
    }

    @Override
    public List<OutboxEvent> buscarPendentes(int limite) {
        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("osId = :pk and begins_with(sk, :prefix)")
                .filterExpression("outboxStatus = :st")
                .expressionAttributeValues(Map.of(
                        ":pk", AttributeValue.fromS(PK),
                        ":prefix", AttributeValue.fromS(SK_PREFIX),
                        ":st", AttributeValue.fromS(StatusOutbox.PENDENTE.name())
                ))
                .build();

        QueryResponse response = dynamoDbClient.query(request);
        return response.items().stream()
                .map(this::fromAttributes)
                .sorted(Comparator.comparing(OutboxEvent::getCreatedAt))
                .limit(limite)
                .toList();
    }

    private OutboxEvent fromAttributes(Map<String, AttributeValue> item) {
        UUID id = UUID.fromString(item.get("id").s());
        String aggregateId = item.get("aggregateId").s();
        String type = item.get("type").s();
        String payload = item.get("payload").s();
        LocalDateTime createdAt = LocalDateTime.parse(item.get("createdAt").s());
        StatusOutbox status = StatusOutbox.valueOf(item.get("outboxStatus").s());
        LocalDateTime sentAt = item.containsKey("sentAt") ? LocalDateTime.parse(item.get("sentAt").s()) : null;

        return new OutboxEvent(id, aggregateId, type, payload, createdAt, status, sentAt);
    }
}
