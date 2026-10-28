package com.fiap.techchallenge.servico.usecase;

import com.fiap.techchallenge.servico.domain.TipoServico;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TipoServicoUseCase {

    List<TipoServico> getAllTiposServico();

    Optional<TipoServico> getTipoServico(Long id);

    /** {@code id} é opcional: quando informado é o refId do serviço no evento DIAGNOSTICO_CONCLUIDO; sem id, é gerado sequencialmente. */
    TipoServico createTipoServico(Long id, String nome, BigDecimal valor);

    TipoServico updateTipoServico(Long id, String nome, BigDecimal valor);

    TipoServico deleteTipoServico(Long id);
}
