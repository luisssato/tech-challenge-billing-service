package com.fiap.techchallenge.servico.usecase;

import com.fiap.techchallenge.servico.domain.TipoServico;

import java.util.Optional;

public interface TipoServicoGateway {

    Optional<TipoServico> findById(Long id);
}
