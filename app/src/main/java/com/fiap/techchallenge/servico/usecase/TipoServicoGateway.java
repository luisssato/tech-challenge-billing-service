package com.fiap.techchallenge.servico.usecase;

import com.fiap.techchallenge.servico.domain.TipoServico;

import java.util.List;
import java.util.Optional;

public interface TipoServicoGateway {

    List<TipoServico> findAll();

    Optional<TipoServico> findById(Long id);

    boolean existsById(Long id);

    TipoServico save(TipoServico tipoServico);

    void deleteById(Long id);

    Long proximoId();
}
