package com.fiap.techchallenge.mensageria.adapter.gateway;

import com.fiap.techchallenge.mensageria.usecase.ProcessedEventGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ProcessedEventGatewayImpl implements ProcessedEventGateway {

    static final String PK = "PROCESSED";
    static final String SK_PREFIX = "EVENT#";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public ProcessedEventGatewayImpl(DynamoDbClient dynamoDbClient,
                                     @Value("${dynamodb.table.name:tech-challenge-prod-billing-service-table}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public boolean jaProcessado(UUID eventId) {
        if (eventId == null) return false;
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "osId", AttributeValue.fromS(PK),
                        "sk", AttributeValue.fromS(SK_PREFIX + eventId)
                ))
                .build();

        GetItemResponse response = dynamoDbClient.getItem(request);
        return response.hasItem() && !response.item().isEmpty();
    }

    @Override
    public void registrarProcessado(UUID eventId, String type, String correlationId) {
        if (eventId == null) return;
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("osId", AttributeValue.fromS(PK));
        item.put("sk", AttributeValue.fromS(SK_PREFIX + eventId));
        item.put("eventId", AttributeValue.fromS(eventId.toString()));
        if (type != null) {
            item.put("type", AttributeValue.fromS(type));
        }
        if (correlationId != null) {
            item.put("correlationId", AttributeValue.fromS(correlationId));
        }
        item.put("processedAt", AttributeValue.fromS(LocalDateTime.now().toString()));

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();
        dynamoDbClient.putItem(request);
    }
}
