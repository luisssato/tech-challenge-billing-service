package com.fiap.techchallenge.observabilidade;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BillingEventLoggerTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void configurarAppender() {
        logger = (Logger) LoggerFactory.getLogger(BillingEventLogger.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void limparAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void registraCamposCanonicosParaEventosRecebidosEProcessados() {
        UUID eventId = UUID.randomUUID();

        BillingEventLogger.eventoRecebido(10L, eventId, "DIAGNOSTICO_CONCLUIDO", "billing-os-events");
        BillingEventLogger.eventoIgnorado(10L, eventId, "EXECUCAO_CONCLUIDA");
        BillingEventLogger.eventoDuplicado(10L, eventId, "DIAGNOSTICO_CONCLUIDO");
        BillingEventLogger.eventoProcessado(10L, eventId, "DIAGNOSTICO_CONCLUIDO",
                "processar_diagnostico", "sucesso", "Diagnóstico processado");
        BillingEventLogger.falhaProcessamento(10L, eventId, "DIAGNOSTICO_CONCLUIDO", "Peça não encontrada");
        BillingEventLogger.eventoInvalido("10", null, eventId.toString(), "OS_CANCELADA", "payload.osId ausente");

        assertEquals(6, appender.list.size());

        ILoggingEvent recebido = appender.list.get(0);
        Map<String, Object> campos = keyValues(recebido);
        assertEquals(Level.INFO, recebido.getLevel());
        assertEquals("billing-service", campos.get("service"));
        assertEquals("10", campos.get("correlationId"));
        assertEquals(10L, campos.get("orderId"));
        assertEquals(eventId.toString(), campos.get("eventId"));
        assertEquals("DIAGNOSTICO_CONCLUIDO", campos.get("eventType"));
        assertEquals("receber_evento", campos.get("operation"));
        assertEquals("recebido", campos.get("outcome"));

        ILoggingEvent falhaProc = appender.list.get(4);
        assertEquals(Level.ERROR, falhaProc.getLevel());
        assertEquals("falha", keyValues(falhaProc).get("outcome"));

        ILoggingEvent invalido = appender.list.get(5);
        assertEquals(Level.ERROR, invalido.getLevel());
        assertEquals(10L, keyValues(invalido).get("orderId"));
        assertEquals("OS_CANCELADA", keyValues(invalido).get("eventType"));
        assertEquals("falha", keyValues(invalido).get("outcome"));
    }

    @Test
    void registraPublicacaoEstornoEDominio() {
        UUID eventId = UUID.randomUUID();

        BillingEventLogger.estornoPagamento(25L, eventId, "EXECUCAO_FALHOU", "estornado", "Pagamento estornado");
        BillingEventLogger.eventoPublicado("25", eventId, "PAGAMENTO_ESTORNADO",
                "publicar_evento_saga", "aceito_broker", "Aceito pelo broker");
        BillingEventLogger.falhaPublicacao("corr-nao-numerico", eventId, "PAGAMENTO_CONFIRMADO",
                "publicar_sqs", "pendente_outbox", "Mantido no outbox", false);
        BillingEventLogger.falhaPublicacao("", null, " ",
                "reenviar_outbox", "falha", "Falha definitiva", true);
        BillingEventLogger.orcamento(25L, "gerar_orcamento", "sucesso", "Orçamento gerado");
        BillingEventLogger.orcamentoAlerta(25L, "aprovar_orcamento", "expirado", "Expirado");
        BillingEventLogger.pagamento(25L, "iniciar_pagamento", "criado", "Cobrança iniciada");
        BillingEventLogger.pagamentoAlerta(null, "processar_webhook", "nao_encontrado", "Não encontrada");

        assertEquals(8, appender.list.size());
        assertEquals(Level.WARN, appender.list.get(0).getLevel());
        assertEquals(25L, keyValues(appender.list.get(1)).get("orderId"));
        assertFalse(keyValues(appender.list.get(2)).containsKey("orderId"));
        assertEquals(Level.WARN, appender.list.get(2).getLevel());
        assertEquals(Level.ERROR, appender.list.get(3).getLevel());
    }

    private static Map<String, Object> keyValues(ILoggingEvent event) {
        return event.getKeyValuePairs().stream()
                .collect(Collectors.toMap(kv -> kv.key, kv -> kv.value));
    }
}
