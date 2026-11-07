package com.fiap.techchallenge.mensageria.adapter.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.mensageria.domain.StatusOutbox;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SnsMensagemPublisherTest {

    private final SnsTemplate snsTemplate = mock(SnsTemplate.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private SnsMensagemPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SnsMensagemPublisher(snsTemplate, mapper, "billing-events");
    }

    @Test
    void publicaMensagemComSucesso() {
        OutboxEvent evento = new OutboxEvent(UUID.randomUUID(), "10", "ORCAMENTO_GERADO", "{\"osId\":\"10\"}", LocalDateTime.now(), StatusOutbox.PENDENTE, null);
        publisher.publicar(evento);

        verify(snsTemplate).send(eq("billing-events"), any(Message.class));
    }

    @Test
    void lancaExcecaoQuandoSnsFalhar() {
        doThrow(new RuntimeException("SNS indisponível")).when(snsTemplate).send(eq("billing-events"), any(Message.class));

        OutboxEvent evento = new OutboxEvent(UUID.randomUUID(), "10", "ORCAMENTO_GERADO", "{\"osId\":\"10\"}", LocalDateTime.now(), StatusOutbox.PENDENTE, null);
        assertThrows(RuntimeException.class, () -> publisher.publicar(evento));
    }
}

