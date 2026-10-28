package com.fiap.techchallenge.sequence;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SequenceGeneratorTest {

    private final DynamoDbClient dynamoDbClient = mock(DynamoDbClient.class);
    private final SequenceGenerator generator = new SequenceGenerator(dynamoDbClient, "tech-challenge-prod-billing-service-table");

    @Test
    void devolveOValorIncrementadoDaSequencia() {
        UpdateItemResponse response = UpdateItemResponse.builder()
                .attributes(Map.of("currentValue", AttributeValue.fromN("7")))
                .build();
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(response);

        assertThat(generator.proximo("tipo_peca")).isEqualTo(7L);

        verify(dynamoDbClient).updateItem(any(UpdateItemRequest.class));
    }
}
