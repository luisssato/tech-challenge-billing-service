package com.fiap.techchallenge.sequence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Map;

@Component
public class SequenceGenerator {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public SequenceGenerator(DynamoDbClient dynamoDbClient,
                             @Value("${dynamodb.table.name:tech-challenge-prod-billing-service-table}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public long proximo(String nome) {
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "osId", AttributeValue.fromS("SEQUENCE"),
                        "sk", AttributeValue.fromS(nome)
                ))
                .updateExpression("ADD currentValue :inc")
                .expressionAttributeValues(Map.of(":inc", AttributeValue.fromN("1")))
                .returnValues(ReturnValue.UPDATED_NEW)
                .build();

        UpdateItemResponse response = dynamoDbClient.updateItem(request);
        return Long.parseLong(response.attributes().get("currentValue").n());
    }
}
