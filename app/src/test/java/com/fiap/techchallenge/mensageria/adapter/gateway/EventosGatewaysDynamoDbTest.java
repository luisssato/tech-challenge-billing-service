package com.fiap.techchallenge.mensageria.adapter.gateway;

import com.fiap.techchallenge.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.mensageria.domain.StatusOutbox;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventosGatewaysDynamoDbTest {

    private final DynamoDbClient client = mock(DynamoDbClient.class);
    private final String table = "tech-challenge-prod-billing-service-table";

    @Test
    void processedEventGatewayVerificaERegistra() {
        ProcessedEventGatewayImpl gateway = new ProcessedEventGatewayImpl(client, table);

        UUID id = UUID.randomUUID();
        when(client.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().item(Map.of("eventId", AttributeValue.fromS(id.toString()))).build());

        assertThat(gateway.jaProcessado(id)).isTrue();
        assertThat(gateway.jaProcessado(null)).isFalse();

        gateway.registrarProcessado(id, "TIPO", "10");
        verify(client).putItem(any(PutItemRequest.class));

        gateway.registrarProcessado(null, "TIPO", "10");
    }

    @Test
    void outboxEventGatewaySalvaEBuscaPendentes() {
        OutboxEventGatewayImpl gateway = new OutboxEventGatewayImpl(client, table);

        UUID id = UUID.randomUUID();
        OutboxEvent evento = new OutboxEvent(id, "10", "TIPO", "{}", LocalDateTime.now(), StatusOutbox.PENDENTE, null);
        gateway.salvar(evento);
        verify(client).putItem(any(PutItemRequest.class));

        Map<String, AttributeValue> item = Map.of(
                "id", AttributeValue.fromS(id.toString()),
                "aggregateId", AttributeValue.fromS("10"),
                "type", AttributeValue.fromS("TIPO"),
                "payload", AttributeValue.fromS("{}"),
                "createdAt", AttributeValue.fromS(LocalDateTime.now().toString()),
                "outboxStatus", AttributeValue.fromS("PENDENTE")
        );
        when(client.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of(item)).build());

        List<OutboxEvent> pendentes = gateway.buscarPendentes(10);
        assertThat(pendentes).hasSize(1);
        assertThat(pendentes.get(0).getId()).isEqualTo(id);
    }
}
