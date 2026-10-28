package com.fiap.techchallenge.mensageria.adapter.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fiap.techchallenge.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.mensageria.usecase.MensagemPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.time.ZoneOffset;

@Component
@Slf4j
public class SqsMensagemPublisher implements MensagemPublisher {

    private static final int VERSAO = 1;

    private final SqsAsyncClient sqs;
    private final ObjectMapper mapper;
    private final String nomeDaFila;
    private volatile String urlDaFila;

    public SqsMensagemPublisher(SqsAsyncClient sqs, ObjectMapper mapper,
                                @Value("${billing.sqs.saida:billing-events}") String nomeDaFila) {
        this.sqs = sqs;
        this.mapper = mapper;
        this.nomeDaFila = nomeDaFila;
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
            sqs.sendMessage(r -> r.queueUrl(urlDaFila()).messageBody(corpo)).join();
            log.info("[SQS PUBLISHER] Mensagem enviada para {}: tipo={}, correlationId={}",
                    nomeDaFila, evento.getType(), evento.getAggregateId());
        } catch (Exception e) {
            log.error("[SQS PUBLISHER] Erro ao enviar mensagem para {}: {}", nomeDaFila, e.getMessage());
            throw new RuntimeException("Falha ao publicar evento no SQS: " + e.getMessage(), e);
        }
    }

    private String urlDaFila() {
        if (urlDaFila == null) {
            urlDaFila = sqs.getQueueUrl(r -> r.queueName(nomeDaFila)).join().queueUrl();
        }
        return urlDaFila;
    }
}
