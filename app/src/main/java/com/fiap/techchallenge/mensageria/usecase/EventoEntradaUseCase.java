package com.fiap.techchallenge.mensageria.usecase;

import com.fiap.techchallenge.orcamento.inputdata.ItemDiagnosticoInputData;

import java.util.List;
import java.util.UUID;

public interface EventoEntradaUseCase {

    void processarDiagnosticoConcluido(UUID eventId, Long osId, List<ItemDiagnosticoInputData> pecas, List<ItemDiagnosticoInputData> servicos);

    void processarOrcamentoAprovado(UUID eventId, Long osId);

    void processarExecucaoFalhou(UUID eventId, Long osId, String motivo, String detalhe);

    void processarOsCancelada(UUID eventId, Long osId, String motivo);
}
