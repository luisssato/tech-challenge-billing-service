package com.fiap.techchallenge.mensageria.adapter.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.mensageria.usecase.EventoSaidaGateway;
import com.fiap.techchallenge.mensageria.usecase.MensagemPublisher;
import com.fiap.techchallenge.mensageria.usecase.OutboxEventGateway;
import com.fiap.techchallenge.observabilidade.BillingEventLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EventoSaidaGatewayImpl implements EventoSaidaGateway {

    private final OutboxEventGateway outboxEventGateway;
    private final ObjectMapper objectMapper;
    private final MensagemPublisher mensagemPublisher;

    public EventoSaidaGatewayImpl(OutboxEventGateway outboxEventGateway,
                                  ObjectMapper objectMapper,
                                  @Autowired(required = false) MensagemPublisher mensagemPublisher) {
        this.outboxEventGateway = outboxEventGateway;
        this.objectMapper = objectMapper;
        this.mensagemPublisher = mensagemPublisher;
    }

    @Override
    public void publicarEvento(String tipo, String correlationId, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEvent evento = OutboxEvent.criar(correlationId, tipo, payloadJson, LocalDateTime.now());
            outboxEventGateway.salvar(evento);

            if (mensagemPublisher != null) {
                try {
                    mensagemPublisher.publicar(evento);
                    evento.marcarComoEnviado(LocalDateTime.now());
                    outboxEventGateway.salvar(evento);
                    BillingEventLogger.eventoPublicado(
                            correlationId,
                            evento.getId(),
                            tipo,
                            "publicar_evento_saga",
                            "aceito_broker",
                            "Evento " + tipo + " aceito pelo broker para entrega (processamento no destino não confirmado)");
                } catch (Exception e) {
                    BillingEventLogger.falhaPublicacao(
                            correlationId,
                            evento.getId(),
                            tipo,
                            "publicar_evento_saga",
                            "pendente_outbox",
                            "Falha ao publicar evento " + tipo + " imediatamente; mantido no outbox para retentativa: " + e.getMessage(),
                            false);
                }
            }
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Erro ao serializar payload do evento " + tipo + ": " + e.getMessage());
        }
    }
}
