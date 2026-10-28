package com.fiap.techchallenge.pagamento.usecase;

import com.fiap.techchallenge.pagamento.domain.Pagamento;

import java.util.Optional;

public interface PagamentoUseCase {

    Optional<Pagamento> getPagamentoPorOs(Long osId);

    Pagamento iniciarPagamento(Long osId);

    Optional<Pagamento> processarNotificacaoWebhook(String idTransacaoProvedor);
}

