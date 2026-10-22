package com.fiap.techchallenge.peca.usecase;

import com.fiap.techchallenge.peca.domain.TipoPeca;

import java.util.Optional;

public interface TipoPecaGateway {

    Optional<TipoPeca> findById(Long id);
}
