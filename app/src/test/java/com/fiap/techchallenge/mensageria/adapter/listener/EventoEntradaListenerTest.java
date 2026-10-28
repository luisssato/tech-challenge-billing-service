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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
}
