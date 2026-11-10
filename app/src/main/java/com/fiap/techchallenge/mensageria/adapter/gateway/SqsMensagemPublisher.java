package com.fiap.techchallenge.mensageria.adapter.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fiap.techchallenge.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.mensageria.domain.TipoEvento;
import com.fiap.techchallenge.mensageria.usecase.MensagemPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "billing.mensageria.modo", havingValue = "sqs", matchIfMissing = true)
@Slf4j
public class SqsMensagemPublisher implements MensagemPublisher {

    private static final int VERSAO = 1;

    private final SqsAsyncClient sqs;
    private final ObjectMapper mapper;
    private final String filaOrcamentoGerado;
    private final String filaPagamentoConfirmadoOs;
    private final String filaPagamentoConfirmadoExecution;
    private final String filaPagamentoFalhou;
    private final String filaPagamentoEstornadoOs;
    private final String filaPagamentoEstornadoExecution;
    private final String filaFallback;

    private final Map<String, String> urlCache = new ConcurrentHashMap<>();

    @Autowired
    public SqsMensagemPublisher(
            SqsAsyncClient sqs,
            ObjectMapper mapper,
            @Value("${billing.sqs.destinos.orcamento-gerado:${billing.sqs.saida:billing-events}}") String filaOrcamentoGerado,
            @Value("${billing.sqs.destinos.pagamento-confirmado-os:${billing.sqs.saida:billing-events}}") String filaPagamentoConfirmadoOs,
            @Value("${billing.sqs.destinos.pagamento-confirmado-execution:${billing.sqs.saida:billing-events}}") String filaPagamentoConfirmadoExecution,
            @Value("${billing.sqs.destinos.pagamento-falhou:${billing.sqs.saida:billing-events}}") String filaPagamentoFalhou,
            @Value("${billing.sqs.destinos.pagamento-estornado-os:${billing.sqs.saida:billing-events}}") String filaPagamentoEstornadoOs,
            @Value("${billing.sqs.destinos.pagamento-estornado-execution:${billing.sqs.saida:billing-events}}") String filaPagamentoEstornadoExecution,
            @Value("${billing.sqs.saida:billing-events}") String filaFallback) {
        this.sqs = sqs;
        this.mapper = mapper;
        this.filaOrcamentoGerado = filaOrcamentoGerado;
        this.filaPagamentoConfirmadoOs = filaPagamentoConfirmadoOs;
        this.filaPagamentoConfirmadoExecution = filaPagamentoConfirmadoExecution;
        this.filaPagamentoFalhou = filaPagamentoFalhou;
        this.filaPagamentoEstornadoOs = filaPagamentoEstornadoOs;
        this.filaPagamentoEstornadoExecution = filaPagamentoEstornadoExecution;
        this.filaFallback = filaFallback;
    }

    public SqsMensagemPublisher(SqsAsyncClient sqs, ObjectMapper mapper, String filaFallback) {
        this(sqs, mapper, filaFallback, filaFallback, filaFallback, filaFallback, filaFallback, filaFallback, filaFallback);
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
            List<String> filasDestino = resolverFilasDestino(evento.getType());

            for (String nomeFila : filasDestino) {
                String url = obterUrlDaFila(nomeFila);
                sqs.sendMessage(r -> r.queueUrl(url).messageBody(corpo)).join();
                log.info("[SQS PUBLISHER] Mensagem enviada para {}: tipo={}, correlationId={}",
                        nomeFila, evento.getType(), evento.getAggregateId());
            }
        } catch (Exception e) {
            log.error("[SQS PUBLISHER] Erro ao enviar mensagem tipo={} para filas destino: {}",
                    evento.getType(), e.getMessage());
            throw new RuntimeException("Falha ao publicar evento no SQS: " + e.getMessage(), e);
        }
    }

    public List<String> resolverFilasDestino(String tipoEvento) {
        if (tipoEvento == null) {
            return List.of(filaFallback);
        }
        return switch (tipoEvento) {
            case TipoEvento.ORCAMENTO_GERADO -> List.of(filaOrcamentoGerado);
            case TipoEvento.PAGAMENTO_CONFIRMADO -> filaPagamentoConfirmadoOs.equals(filaPagamentoConfirmadoExecution)
                    ? List.of(filaPagamentoConfirmadoOs)
                    : List.of(filaPagamentoConfirmadoOs, filaPagamentoConfirmadoExecution);
            case TipoEvento.PAGAMENTO_FALHOU -> List.of(filaPagamentoFalhou);
            case TipoEvento.PAGAMENTO_ESTORNADO -> filaPagamentoEstornadoOs.equals(filaPagamentoEstornadoExecution)
                    ? List.of(filaPagamentoEstornadoOs)
                    : List.of(filaPagamentoEstornadoOs, filaPagamentoEstornadoExecution);
            case TipoEvento.ORCAMENTO_REJEITADO -> List.of(filaOrcamentoGerado);
            default -> List.of(filaFallback);
        };
    }

    private String obterUrlDaFila(String nomeFila) {
        return urlCache.computeIfAbsent(nomeFila, nome ->
                sqs.getQueueUrl(r -> r.queueName(nome)).join().queueUrl()
        );
    }
}
