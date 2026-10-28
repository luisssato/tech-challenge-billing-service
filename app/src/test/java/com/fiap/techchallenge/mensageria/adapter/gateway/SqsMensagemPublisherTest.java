package com.fiap.techchallenge.mensageria.adapter.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.mensageria.domain.StatusOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SqsMensagemPublisherTest {

    private final SqsAsyncClient sqs = mock(SqsAsyncClient.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private SqsMensagemPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SqsMensagemPublisher(sqs, mapper, "fila-saida");
        GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl("http://sqs/fila-saida").build();
        when(sqs.getQueueUrl(any(Consumer.class))).thenReturn(CompletableFuture.completedFuture(urlResponse));
    }

    @Test
    void publicaMensagemComSucesso() {
        SendMessageResponse sendResponse = SendMessageResponse.builder().messageId("msg-1").build();
        when(sqs.sendMessage(any(Consumer.class))).thenReturn(CompletableFuture.completedFuture(sendResponse));

        OutboxEvent evento = new OutboxEvent(UUID.randomUUID(), "10", "TESTE", "{\"k\":\"v\"}", LocalDateTime.now(), StatusOutbox.PENDENTE, null);
        publisher.publicar(evento);

        verify(sqs).sendMessage(any(Consumer.class));
    }

    @Test
    void lancaExcecaoQuandoSqsFalhar() {
        when(sqs.sendMessage(any(Consumer.class))).thenReturn(CompletableFuture.failedFuture(new RuntimeException("SQS error")));

        OutboxEvent evento = new OutboxEvent(UUID.randomUUID(), "10", "TESTE", "{\"k\":\"v\"}", LocalDateTime.now(), StatusOutbox.PENDENTE, null);
        assertThrows(RuntimeException.class, () -> publisher.publicar(evento));
    }
}
