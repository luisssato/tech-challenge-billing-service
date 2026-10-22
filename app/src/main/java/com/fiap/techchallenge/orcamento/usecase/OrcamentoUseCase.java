package com.fiap.techchallenge.orcamento.usecase;

import com.fiap.techchallenge.orcamento.domain.Orcamento;
import com.fiap.techchallenge.orcamento.inputdata.ItemDiagnosticoInputData;

import java.util.List;
import java.util.Optional;

public interface OrcamentoUseCase {

    Optional<Orcamento> getOrcamentoPorOs(Long osId);

    Orcamento gerarOrcamento(Long osId, List<ItemDiagnosticoInputData> pecas, List<ItemDiagnosticoInputData> servicos);

    Orcamento aprovarOrcamento(Long osId);

    Orcamento rejeitarOrcamento(Long osId);
}
