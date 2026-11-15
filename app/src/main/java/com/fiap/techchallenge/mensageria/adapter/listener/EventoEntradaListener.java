package com.fiap.techchallenge.mensageria.adapter.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.mensageria.usecase.EventoEntradaUseCase;
import com.fiap.techchallenge.observabilidade.BillingEventLogger;
import com.fiap.techchallenge.orcamento.inputdata.ItemDiagnosticoInputData;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class EventoEntradaListener {

    private final EventoEntradaUseCase eventos;
    private final ObjectMapper mapper;

    public EventoEntradaListener(EventoEntradaUseCase eventos, ObjectMapper mapper) {
        this.eventos = eventos;
        this.mapper = mapper;
    }

    @SqsListener("${billing.sqs.fila-os:billing-service-os-events}")
    public void consumirFilaOs(String mensagem) {
        EnvelopeEntrada envelope = ler(mensagem);
        String tipo = envelope.type();
        BillingEventLogger.eventoRecebido(envelope.osId(), envelope.eventId(), tipo, "billing-service-os-events");

        if ("ORCAMENTO_APROVADO".equalsIgnoreCase(tipo)) {
            aoAprovarOrcamento(envelope);
        } else if ("OS_CANCELADA".equalsIgnoreCase(tipo) || "ORCAMENTO_REJEITADO".equalsIgnoreCase(tipo)) {
            aoCancelarOs(envelope);
        } else {
            BillingEventLogger.eventoIgnorado(envelope.osId(), envelope.eventId(), tipo);
        }
    }

    @SqsListener("${billing.sqs.fila-execution:billing-service-execution-events}")
    public void consumirFilaExecution(String mensagem) {
        EnvelopeEntrada envelope = ler(mensagem);
        String tipo = envelope.type();
        BillingEventLogger.eventoRecebido(envelope.osId(), envelope.eventId(), tipo, "billing-service-execution-events");

        if ("DIAGNOSTICO_CONCLUIDO".equalsIgnoreCase(tipo)) {
            aoConcluirDiagnostico(envelope);
        } else if ("EXECUCAO_FALHOU".equalsIgnoreCase(tipo)) {
            aoFalharExecucao(envelope);
        } else {
            BillingEventLogger.eventoIgnorado(envelope.osId(), envelope.eventId(), tipo);
        }
    }

    public void aoConcluirDiagnostico(String mensagem) {
        aoConcluirDiagnostico(ler(mensagem));
    }

    public void aoConcluirDiagnostico(EnvelopeEntrada envelope) {
        Long osId = envelope.osId();
        List<ItemDiagnosticoInputData> pecas = new ArrayList<>();
        List<ItemDiagnosticoInputData> servicos = new ArrayList<>();

        JsonNode itensNode = envelope.payload().path("itens");
        if (itensNode.isArray()) {
            for (JsonNode item : itensNode) {
                String tipo = item.path("tipo").asText("");
                long refId = item.path("refId").asLong();
                int quantidade = item.path("quantidade").asInt(1);

                ItemDiagnosticoInputData input = new ItemDiagnosticoInputData(refId, quantidade);
                if ("PECA".equalsIgnoreCase(tipo)) {
                    pecas.add(input);
                } else if ("SERVICO".equalsIgnoreCase(tipo)) {
                    servicos.add(input);
                }
            }
        }

        executarProcessamento(envelope, () ->
                eventos.processarDiagnosticoConcluido(envelope.eventId(), osId, pecas, servicos));
    }

    public void aoAprovarOrcamento(String mensagem) {
        aoAprovarOrcamento(ler(mensagem));
    }

    public void aoAprovarOrcamento(EnvelopeEntrada envelope) {
        Long osId = envelope.osId();
        executarProcessamento(envelope, () ->
                eventos.processarOrcamentoAprovado(envelope.eventId(), osId));
    }

    public void aoFalharExecucao(String mensagem) {
        aoFalharExecucao(ler(mensagem));
    }

    public void aoFalharExecucao(EnvelopeEntrada envelope) {
        Long osId = envelope.osId();
        String motivo = envelope.payload().path("motivo").asText("FALHA_NA_EXECUCAO");
        String detalhe = envelope.payload().path("detalhe").asText("");

        executarProcessamento(envelope, () ->
                eventos.processarExecucaoFalhou(envelope.eventId(), osId, motivo, detalhe));
    }

    public void aoCancelarOs(String mensagem) {
        aoCancelarOs(ler(mensagem));
    }

    public void aoCancelarOs(EnvelopeEntrada envelope) {
        Long osId = envelope.osId();
        String motivo = envelope.payload().path("motivo").asText("CANCELADA");

        executarProcessamento(envelope, () ->
                eventos.processarOsCancelada(envelope.eventId(), osId, motivo));
    }

    private void executarProcessamento(EnvelopeEntrada envelope, Runnable acao) {
        try {
            acao.run();
        } catch (RuntimeException e) {
            BillingEventLogger.falhaProcessamento(envelope.osId(), envelope.eventId(), envelope.type(), e.getMessage());
            throw e;
        }
    }

    private EnvelopeEntrada ler(String mensagem) {
        String correlationIdExtraido = null;
        Long osIdExtraido = null;
        String eventIdExtraido = null;
        String tipoExtraido = null;
        try {
            JsonNode raiz = mapper.readTree(mensagem);
            correlationIdExtraido = textoOuNulo(raiz.path("correlationId"));
            eventIdExtraido = textoOuNulo(raiz.path("eventId"));
            tipoExtraido = textoOuNulo(raiz.path("type"));
            JsonNode payload = raiz.path("payload");
            if (payload.hasNonNull("osId") && payload.path("osId").canConvertToLong()) {
                osIdExtraido = payload.path("osId").asLong();
            }

            UUID eventId = UUID.fromString(raiz.path("eventId").asText());
            if (payload.isMissingNode() || !payload.hasNonNull("osId")) {
                throw new BadRequestException("Mensagem inválida: payload.osId ausente!");
            }
            long osId = payload.path("osId").asLong();
            return new EnvelopeEntrada(eventId, raiz.path("type").asText(""), osId, payload);
        } catch (BadRequestException e) {
            BillingEventLogger.eventoInvalido(correlationIdExtraido, osIdExtraido, eventIdExtraido, tipoExtraido, e.getMessage());
            throw e;
        } catch (IllegalArgumentException | com.fasterxml.jackson.core.JacksonException e) {
            BillingEventLogger.eventoInvalido(correlationIdExtraido, osIdExtraido, eventIdExtraido, tipoExtraido, e.getMessage());
            throw new BadRequestException("Mensagem SQS inválida: " + e.getMessage());
        }
    }

    private static String textoOuNulo(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String valor = node.asText("");
        return valor.isBlank() ? null : valor;
    }

    record EnvelopeEntrada(UUID eventId, String type, Long osId, JsonNode payload) {}
}
