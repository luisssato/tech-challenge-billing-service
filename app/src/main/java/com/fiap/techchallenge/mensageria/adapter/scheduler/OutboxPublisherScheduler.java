package com.fiap.techchallenge.mensageria.adapter.scheduler;

import com.fiap.techchallenge.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.mensageria.usecase.MensagemPublisher;
import com.fiap.techchallenge.mensageria.usecase.OutboxEventGateway;
import com.fiap.techchallenge.observabilidade.BillingEventLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "billing.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisherScheduler {

    private final OutboxEventGateway outboxGateway;
    private final MensagemPublisher publisher;
    private final int lote;

    public OutboxPublisherScheduler(OutboxEventGateway outboxGateway,
                                    @Autowired(required = false) MensagemPublisher publisher,
                                    @Value("${billing.outbox.lote:50}") int lote) {
        this.outboxGateway = outboxGateway;
        this.publisher = publisher;
        this.lote = lote;
    }

    @Scheduled(fixedDelayString = "${billing.outbox.intervalo-ms:1000}")
    public void publicarPendentes() {
        if (publisher == null) return;

        List<OutboxEvent> pendentes = outboxGateway.buscarPendentes(lote);
        for (OutboxEvent evento : pendentes) {
            try {
                publisher.publicar(evento);
                evento.marcarComoEnviado(LocalDateTime.now());
                outboxGateway.salvar(evento);
                BillingEventLogger.eventoPublicado(
                        evento.getAggregateId(),
                        evento.getId(),
                        evento.getType(),
                        "reenviar_outbox",
                        "aceito_broker",
                        "Evento outbox reenviado e aceito pelo broker");
            } catch (Exception e) {
                evento.marcarComoFalha();
                outboxGateway.salvar(evento);
                BillingEventLogger.falhaPublicacao(
                        evento.getAggregateId(),
                        evento.getId(),
                        evento.getType(),
                        "reenviar_outbox",
                        "falha",
                        "Falha ao processar evento outbox " + evento.getId() + ": " + e.getMessage(),
                        true);
            }
        }
    }
}
