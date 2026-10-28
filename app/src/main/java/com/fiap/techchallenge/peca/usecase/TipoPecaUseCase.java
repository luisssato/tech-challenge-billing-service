package com.fiap.techchallenge.peca.usecase;

import com.fiap.techchallenge.peca.domain.TipoPeca;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TipoPecaUseCase {

    List<TipoPeca> getAllTiposPeca();

    Optional<TipoPeca> getTipoPeca(Long id);

    TipoPeca createTipoPeca(Long id, String nome, BigDecimal valorUnitario);

    TipoPeca updateTipoPeca(Long id, String nome, BigDecimal valorUnitario);

    TipoPeca deleteTipoPeca(Long id);
}
