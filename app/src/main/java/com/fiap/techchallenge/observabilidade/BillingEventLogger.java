package com.fiap.techchallenge.observabilidade;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.UUID;

/**
 * Emite logs estruturados em JSON seguindo a convenção de observabilidade dos microsserviços.
 */
@Slf4j
public final class BillingEventLogger {

    public static final String SERVICE_NAME = "billing-service";

    private BillingEventLogger() {
    }

    public static void eventoRecebido(Long osId, UUID eventId, String eventType, String fila) {
        emitir(log.atInfo(), textoOuNulo(osId), osId, textoOuNulo(eventId), eventType,
                "receber_evento", "recebido", "Evento recebido na fila " + fila);
    }

    public static void eventoIgnorado(Long osId, UUID eventId, String eventType) {
        emitir(log.atInfo(), textoOuNulo(osId), osId, textoOuNulo(eventId), eventType,
                "processar_evento", "ignorado", "Evento ignorado por não ser tratado pelo billing-service");
    }

    public static void eventoInvalido(String correlationId, Long osId, String eventId, String eventType, String motivo) {
        emitir(log.atError(), correlationId, osId, eventId, eventType,
                "receber_evento", "falha", "Mensagem SQS com formato inválido: " + motivo);
    }

    public static void eventoDuplicado(Long osId, UUID eventId, String eventType) {
        emitir(log.atInfo(), textoOuNulo(osId), osId, textoOuNulo(eventId), eventType,
                "processar_evento", "duplicado", "Evento já processado anteriormente; duplicidade ignorada");
    }

    public static void eventoProcessado(Long osId, UUID eventId, String eventType,
                                        String operation, String outcome, String mensagem) {
        emitir(log.atInfo(), textoOuNulo(osId), osId, textoOuNulo(eventId), eventType,
                operation, outcome, mensagem);
    }

    public static void falhaProcessamento(Long osId, UUID eventId, String eventType, String motivo) {
        emitir(log.atError(), textoOuNulo(osId), osId, textoOuNulo(eventId), eventType,
                "processar_evento", "falha", "Falha ao processar evento: " + motivo);
    }

    public static void estornoPagamento(Long osId, UUID eventId, String eventType,
                                        String outcome, String mensagem) {
        emitir(log.atWarn(), textoOuNulo(osId), osId, textoOuNulo(eventId), eventType,
                "estornar_pagamento", outcome, mensagem);
    }

    public static void eventoPublicado(String correlationId, UUID eventId, String eventType,
                                       String operation, String outcome, String mensagem) {
        emitir(log.atInfo(), correlationId, parseOrderId(correlationId), textoOuNulo(eventId), eventType,
                operation, outcome, mensagem);
    }

    public static void falhaPublicacao(String correlationId, UUID eventId, String eventType,
                                       String operation, String outcome, String mensagem, boolean erroCritico) {
        LoggingEventBuilder builder = erroCritico ? log.atError() : log.atWarn();
        emitir(builder, correlationId, parseOrderId(correlationId), textoOuNulo(eventId), eventType,
                operation, outcome, mensagem);
    }

    public static void orcamento(Long osId, String operation, String outcome, String mensagem) {
        emitir(log.atInfo(), textoOuNulo(osId), osId, null, null,
                operation, outcome, mensagem);
    }

    public static void orcamentoAlerta(Long osId, String operation, String outcome, String mensagem) {
        emitir(log.atWarn(), textoOuNulo(osId), osId, null, null,
                operation, outcome, mensagem);
    }

    public static void pagamento(Long osId, String operation, String outcome, String mensagem) {
        emitir(log.atInfo(), textoOuNulo(osId), osId, null, null,
                operation, outcome, mensagem);
    }

    public static void pagamentoAlerta(Long osId, String operation, String outcome, String mensagem) {
        emitir(log.atWarn(), textoOuNulo(osId), osId, null, null,
                operation, outcome, mensagem);
    }

    private static void emitir(LoggingEventBuilder builder, String correlationId, Long orderId,
                               String eventId, String eventType, String operation, String outcome, String mensagem) {
        builder.addKeyValue("service", SERVICE_NAME);
        String corr = resolverCorrelationId(correlationId, orderId);
        if (corr != null) {
            builder.addKeyValue("correlationId", corr);
        }
        Long ord = orderId != null ? orderId : parseOrderId(corr);
        if (ord != null) {
            builder.addKeyValue("orderId", ord);
        }
        if (eventId != null && !eventId.isBlank()) {
            builder.addKeyValue("eventId", eventId);
        }
        if (eventType != null && !eventType.isBlank()) {
            builder.addKeyValue("eventType", eventType);
        }
        builder.addKeyValue("operation", operation)
                .addKeyValue("outcome", outcome)
                .log(mensagem);
    }

    private static String resolverCorrelationId(String correlationId, Long orderId) {
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        return orderId != null ? String.valueOf(orderId) : null;
    }

    private static String textoOuNulo(Object valor) {
        return valor != null ? String.valueOf(valor) : null;
    }

    private static Long parseOrderId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(correlationId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
