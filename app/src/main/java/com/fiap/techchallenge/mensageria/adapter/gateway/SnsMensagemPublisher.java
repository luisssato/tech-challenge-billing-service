package com.fiap.techchallenge.mensageria.adapter.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fiap.techchallenge.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.mensageria.usecase.MensagemPublisher;
import io.awspring.cloud.sns.core.SnsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
@ConditionalOnProperty(name = "billing.mensageria.modo", havingValue = "sns")
@Slf4j
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
            log.info("[SNS PUBLISHER] Mensagem enviada para tópico {}: tipo={}, correlationId={}",
                    nomeDoTopico, evento.getType(), evento.getAggregateId());
        } catch (Exception e) {
            log.error("[SNS PUBLISHER] Erro ao enviar mensagem para {}: {}", nomeDoTopico, e.getMessage());
            throw new RuntimeException("Falha ao publicar evento no SNS: " + e.getMessage(), e);
        }
    }
}
