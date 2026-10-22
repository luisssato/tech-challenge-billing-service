package com.fiap.techchallenge.orcamento.usecase;

import com.fiap.techchallenge.orcamento.domain.Orcamento;

import java.util.Optional;

public interface OrcamentoGateway {

    Optional<Orcamento> findByOsId(Long osId);

    Orcamento save(Orcamento orcamento);
}
