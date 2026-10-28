package com.fiap.techchallenge.mensageria.adapter.scheduler;

import com.fiap.techchallenge.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.mensageria.domain.StatusOutbox;
import com.fiap.techchallenge.mensageria.usecase.MensagemPublisher;
import com.fiap.techchallenge.mensageria.usecase.OutboxEventGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OutboxPublisherSchedulerTest {

    private final OutboxEventGateway outboxGateway = mock(OutboxEventGateway.class);
    private final MensagemPublisher publisher = mock(MensagemPublisher.class);
    private OutboxPublisherScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OutboxPublisherScheduler(outboxGateway, publisher, 10);
    }

    @Test
    void publicaEventosPendentesEAtualizaStatus() {
        OutboxEvent evento = new OutboxEvent(UUID.randomUUID(), "10", "TIPO", "{}", LocalDateTime.now(), StatusOutbox.PENDENTE, null);
        when(outboxGateway.buscarPendentes(10)).thenReturn(List.of(evento));

        scheduler.publicarPendentes();

        verify(publisher).publicar(evento);
        assertThat(evento.getStatus()).isEqualTo(StatusOutbox.ENVIADO);
        verify(outboxGateway).salvar(evento);
    }

    @Test
    void marcaComoFalhaQuandoPublisherLancarExcecao() {
        OutboxEvent evento = new OutboxEvent(UUID.randomUUID(), "10", "TIPO", "{}", LocalDateTime.now(), StatusOutbox.PENDENTE, null);
        when(outboxGateway.buscarPendentes(10)).thenReturn(List.of(evento));
        doThrow(new RuntimeException("SQS down")).when(publisher).publicar(evento);

        scheduler.publicarPendentes();

        assertThat(evento.getStatus()).isEqualTo(StatusOutbox.FALHA);
        verify(outboxGateway).salvar(evento);
    }
}
