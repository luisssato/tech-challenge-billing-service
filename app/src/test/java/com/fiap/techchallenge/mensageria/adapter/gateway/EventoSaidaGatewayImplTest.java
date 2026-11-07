package com.fiap.techchallenge.mensageria.adapter.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.mensageria.domain.StatusOutbox;
import com.fiap.techchallenge.mensageria.usecase.MensagemPublisher;
import com.fiap.techchallenge.mensageria.usecase.OutboxEventGateway;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventoSaidaGatewayImplTest {

    private final OutboxEventGateway outboxGateway = mock(OutboxEventGateway.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MensagemPublisher publisher = mock(MensagemPublisher.class);

    @Test
    void publicaEventoComSucessoDiretoNoPublisher() {
        EventoSaidaGatewayImpl gateway = new EventoSaidaGatewayImpl(outboxGateway, objectMapper, publisher);

        gateway.publicarEvento("TESTE", "10", Map.of("foo", "bar"));

        verify(outboxGateway, atLeastOnce()).salvar(any(OutboxEvent.class));
        verify(publisher).publicar(any(OutboxEvent.class));
    }

    @Test
    void toleraFalhaNoPublisherDiretoDeixandoParaOutbox() {
        EventoSaidaGatewayImpl gateway = new EventoSaidaGatewayImpl(outboxGateway, objectMapper, publisher);
        doThrow(new RuntimeException("Offline")).when(publisher).publicar(any());

        gateway.publicarEvento("TESTE", "10", Map.of("foo", "bar"));

        verify(outboxGateway).salvar(any(OutboxEvent.class));
    }

    @Test
    void salvaNaOutboxQuandoNaoHouverPublisherConfigurado() {
        EventoSaidaGatewayImpl gateway = new EventoSaidaGatewayImpl(outboxGateway, objectMapper, null);

        gateway.publicarEvento("TESTE", "10", Map.of("foo", "bar"));

        verify(outboxGateway).salvar(any(OutboxEvent.class));
    }
}
