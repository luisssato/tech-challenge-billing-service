package com.fiap.techchallenge.mensageria.adapter.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.mensageria.usecase.EventoEntradaUseCase;
import com.fiap.techchallenge.orcamento.inputdata.ItemDiagnosticoInputData;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
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
        log.info("[SQS LISTENER - OS] Recebido tipo={} para OS {} (eventId: {})", tipo, envelope.osId(), envelope.eventId());

        if ("ORCAMENTO_APROVADO".equalsIgnoreCase(tipo)) {
            aoAprovarOrcamento(envelope);
        } else if ("OS_CANCELADA".equalsIgnoreCase(tipo) || "ORCAMENTO_REJEITADO".equalsIgnoreCase(tipo)) {
            aoCancelarOs(envelope);
        } else {
            log.info("[SQS LISTENER - OS] Evento {} ignorado pelo billing", tipo);
        }
    }

    @SqsListener("${billing.sqs.fila-execution:billing-service-execution-events}")
    public void consumirFilaExecution(String mensagem) {
        EnvelopeEntrada envelope = ler(mensagem);
        String tipo = envelope.type();
        log.info("[SQS LISTENER - EXECUTION] Recebido tipo={} para OS {} (eventId: {})", tipo, envelope.osId(), envelope.eventId());

        if ("DIAGNOSTICO_CONCLUIDO".equalsIgnoreCase(tipo)) {
            aoConcluirDiagnostico(envelope);
        } else if ("EXECUCAO_FALHOU".equalsIgnoreCase(tipo)) {
            aoFalharExecucao(envelope);
        } else {
            log.info("[SQS LISTENER - EXECUTION] Evento {} ignorado pelo billing", tipo);
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

        log.info("[SQS LISTENER] Processando DIAGNOSTICO_CONCLUIDO para OS {} (eventId: {})", osId, envelope.eventId());
        eventos.processarDiagnosticoConcluido(envelope.eventId(), osId, pecas, servicos);
    }

    public void aoAprovarOrcamento(String mensagem) {
        aoAprovarOrcamento(ler(mensagem));
    }

    public void aoAprovarOrcamento(EnvelopeEntrada envelope) {
        Long osId = envelope.osId();
        log.info("[SQS LISTENER] Processando ORCAMENTO_APROVADO para OS {} (eventId: {})", osId, envelope.eventId());
        eventos.processarOrcamentoAprovado(envelope.eventId(), osId);
    }

    public void aoFalharExecucao(String mensagem) {
        aoFalharExecucao(ler(mensagem));
    }

    public void aoFalharExecucao(EnvelopeEntrada envelope) {
        Long osId = envelope.osId();
        String motivo = envelope.payload().path("motivo").asText("FALHA_NA_EXECUCAO");
        String detalhe = envelope.payload().path("detalhe").asText("");

        log.info("[SQS LISTENER] Processando EXECUCAO_FALHOU para OS {} (eventId: {}, motivo: {})", osId, envelope.eventId(), motivo);
        eventos.processarExecucaoFalhou(envelope.eventId(), osId, motivo, detalhe);
    }

    public void aoCancelarOs(String mensagem) {
        aoCancelarOs(ler(mensagem));
    }

    public void aoCancelarOs(EnvelopeEntrada envelope) {
        Long osId = envelope.osId();
        String motivo = envelope.payload().path("motivo").asText("CANCELADA");

        log.info("[SQS LISTENER] Processando OS_CANCELADA para OS {} (eventId: {})", osId, envelope.eventId());
        eventos.processarOsCancelada(envelope.eventId(), osId, motivo);
    }

    private EnvelopeEntrada ler(String mensagem) {
        try {
            JsonNode raiz = mapper.readTree(mensagem);
            UUID eventId = UUID.fromString(raiz.path("eventId").asText());
            JsonNode payload = raiz.path("payload");
            if (payload.isMissingNode() || !payload.hasNonNull("osId")) {
                throw new BadRequestException("Mensagem inválida: payload.osId ausente!");
            }
            long osId = payload.path("osId").asLong();
            return new EnvelopeEntrada(eventId, raiz.path("type").asText(""), osId, payload);
        } catch (IllegalArgumentException | com.fasterxml.jackson.core.JacksonException e) {
            log.error("[SQS LISTENER] Mensagem com formato inválido: {}", mensagem, e);
            throw new BadRequestException("Mensagem SQS inválida: " + e.getMessage());
        }
    }

    record EnvelopeEntrada(UUID eventId, String type, Long osId, JsonNode payload) {}
}
