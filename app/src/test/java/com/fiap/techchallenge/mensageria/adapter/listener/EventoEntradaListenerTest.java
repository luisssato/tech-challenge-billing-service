package com.fiap.techchallenge.mensageria.adapter.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.mensageria.usecase.EventoEntradaUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventoEntradaListenerTest {

    private final EventoEntradaUseCase useCase = mock(EventoEntradaUseCase.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private EventoEntradaListener listener;

    @BeforeEach
    void setUp() {
        listener = new EventoEntradaListener(useCase, mapper);
    }

    @Test
    void processaDiagnosticoConcluidoComItensCorretamente() {
        UUID eventId = UUID.randomUUID();
        String json = """
                {
                  "eventId": "%s",
                  "type": "DIAGNOSTICO_CONCLUIDO",
                  "version": 1,
                  "correlationId": "100",
                  "payload": {
                    "osId": 100,
                    "itens": [
                      { "tipo": "PECA", "refId": 5, "quantidade": 2 },
                      { "tipo": "SERVICO", "refId": 8, "quantidade": 1 }
                    ]
                  }
                }
                """.formatted(eventId);

        listener.aoConcluirDiagnostico(json);

        verify(useCase).processarDiagnosticoConcluido(eq(eventId), eq(100L), any(), any());
    }

    @Test
    void processaExecucaoFalhouCorretamente() {
        UUID eventId = UUID.randomUUID();
        String json = """
                {
                  "eventId": "%s",
                  "type": "EXECUCAO_FALHOU",
                  "payload": {
                    "osId": 100,
                    "motivo": "SEM_ESTOQUE",
                    "detalhe": "Peça 5 zerada"
                  }
                }
                """.formatted(eventId);

        listener.aoFalharExecucao(json);

        verify(useCase).processarExecucaoFalhou(eventId, 100L, "SEM_ESTOQUE", "Peça 5 zerada");
    }

    @Test
    void processaOsCanceladaCorretamente() {
        UUID eventId = UUID.randomUUID();
        String json = """
                {
                  "eventId": "%s",
                  "type": "OS_CANCELADA",
                  "payload": {
                    "osId": 100,
                    "motivo": "CLIENTE_REJEITOU"
                  }
                }
                """.formatted(eventId);

        listener.aoCancelarOs(json);

        verify(useCase).processarOsCancelada(eventId, 100L, "CLIENTE_REJEITOU");
    }

    @Test
    void processaOrcamentoAprovadoCorretamente() {
        UUID eventId = UUID.randomUUID();
        String json = """
                {
                  "eventId": "%s",
                  "type": "ORCAMENTO_APROVADO",
                  "payload": {
                    "osId": 100
                  }
                }
                """.formatted(eventId);

        listener.aoAprovarOrcamento(json);

        verify(useCase).processarOrcamentoAprovado(eventId, 100L);
    }

    @Test
    void consumirFilaOsRoteiaAprovacaoECancelamentoEIgnoraDesconhecido() {
        UUID id1 = UUID.randomUUID();
        String msgAprovado = """
                {"eventId":"%s","type":"ORCAMENTO_APROVADO","payload":{"osId":101}}
                """.formatted(id1);
        listener.consumirFilaOs(msgAprovado);
        verify(useCase).processarOrcamentoAprovado(id1, 101L);

        UUID id2 = UUID.randomUUID();
        String msgCancelada = """
                {"eventId":"%s","type":"OS_CANCELADA","payload":{"osId":102,"motivo":"DESISTENCIA"}}
                """.formatted(id2);
        listener.consumirFilaOs(msgCancelada);
        verify(useCase).processarOsCancelada(id2, 102L, "DESISTENCIA");

        UUID id3 = UUID.randomUUID();
        String msgRejeitado = """
                {"eventId":"%s","type":"ORCAMENTO_REJEITADO","payload":{"osId":103}}
                """.formatted(id3);
        listener.consumirFilaOs(msgRejeitado);
        verify(useCase).processarOsCancelada(id3, 103L, "CANCELADA");

        String msgIgnorada = """
                {"eventId":"%s","type":"OS_CRIADA","payload":{"osId":104}}
                """.formatted(UUID.randomUUID());
        listener.consumirFilaOs(msgIgnorada);
    }

    @Test
    void consumirFilaExecutionRoteiaDiagnosticoEFalhaEIgnoraDesconhecido() {
        UUID id1 = UUID.randomUUID();
        String msgDiag = """
                {
                  "eventId": "%s",
                  "type": "DIAGNOSTICO_CONCLUIDO",
                  "payload": {
                    "osId": 201,
                    "itens": [{"tipo": "PECA", "refId": 1, "quantidade": 2}]
                  }
                }
                """.formatted(id1);
        listener.consumirFilaExecution(msgDiag);
        verify(useCase).processarDiagnosticoConcluido(eq(id1), eq(201L), any(), any());

        UUID id2 = UUID.randomUUID();
        String msgFalha = """
                {
                  "eventId": "%s",
                  "type": "EXECUCAO_FALHOU",
                  "payload": {"osId": 202, "motivo": "FALHA_MOTOR"}
                }
                """.formatted(id2);
        listener.consumirFilaExecution(msgFalha);
        verify(useCase).processarExecucaoFalhou(id2, 202L, "FALHA_MOTOR", "");

        String msgIgnorada = """
                {"eventId":"%s","type":"EXECUCAO_INICIADA","payload":{"osId":203}}
                """.formatted(UUID.randomUUID());
        listener.consumirFilaExecution(msgIgnorada);
    }

    @Test
    void lancaBadRequestQuandoMensagemSemOsId() {
        String json = """
                {
                  "eventId": "%s",
                  "type": "OS_CANCELADA",
                  "payload": {}
                }
                """.formatted(UUID.randomUUID());

        assertThrows(BadRequestException.class, () -> listener.aoCancelarOs(json));
    }

    @Test
    void lancaBadRequestQuandoJsonInvalido() {
        assertThrows(BadRequestException.class, () -> listener.aoCancelarOs("invalido"));
    }

    @Test
    void propagaExcecaoDeProcessamentoDoUseCase() {
        UUID eventId = UUID.randomUUID();
        doThrow(new RuntimeException("Peça 5 não cadastrada"))
                .when(useCase).processarOrcamentoAprovado(eventId, 100L);
        String json = """
                {
                  "eventId": "%s",
                  "type": "ORCAMENTO_APROVADO",
                  "payload": { "osId": 100 }
                }
                """.formatted(eventId);

        assertThrows(RuntimeException.class, () -> listener.aoAprovarOrcamento(json));
    }
}
