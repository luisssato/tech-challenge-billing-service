package com.fiap.techchallenge.mensageria.adapter.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fiap.techchallenge.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.mensageria.usecase.MensagemPublisher;
import com.fiap.techchallenge.observabilidade.BillingEventLogger;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
@ConditionalOnProperty(name = "billing.mensageria.modo", havingValue = "sns")
public class SnsMensagemPublisher implements MensagemPublisher {

    private static final int VERSAO = 1;

    private final SnsTemplate snsTemplate;
    private final ObjectMapper mapper;
    private final String nomeDoTopico;

    public SnsMensagemPublisher(SnsTemplate snsTemplate,
                                ObjectMapper mapper,
                                @Value("${billing.sns.saida:billing-events}") String nomeDoTopico) {
        this.snsTemplate = snsTemplate;
        this.mapper = mapper;
        this.nomeDoTopico = nomeDoTopico;
    }

    @Override
    public void publicar(OutboxEvent evento) {
        try {
            ObjectNode envelope = mapper.createObjectNode();
            envelope.put("eventId", evento.getId().toString());
            envelope.put("type", evento.getType());
            envelope.put("version", VERSAO);
            envelope.put("occurredAt", evento.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toString());
            envelope.put("correlationId", String.valueOf(evento.getAggregateId()));
            envelope.set("payload", mapper.readTree(evento.getPayload()));

            String corpo = mapper.writeValueAsString(envelope);
            snsTemplate.send(nomeDoTopico, MessageBuilder.withPayload(corpo)
                    .setHeader("eventId", evento.getId().toString())
                    .setHeader("type", evento.getType())
                    .setHeader("correlationId", String.valueOf(evento.getAggregateId()))
                    .build());
            BillingEventLogger.eventoPublicado(
                    evento.getAggregateId(),
                    evento.getId(),
                    evento.getType(),
                    "publicar_sns",
                    "aceito_broker",
                    "Mensagem aceita pelo tópico SNS " + nomeDoTopico);
        } catch (Exception e) {
            BillingEventLogger.falhaPublicacao(
                    evento.getAggregateId(),
                    evento.getId(),
                    evento.getType(),
                    "publicar_sns",
                    "falha",
                    "Erro ao enviar mensagem para tópico SNS " + nomeDoTopico + ": " + e.getMessage(),
                    true);
            throw new RuntimeException("Falha ao publicar evento no SNS: " + e.getMessage(), e);
        }
    }
}
