package com.fiap.techchallenge.pagamento.usecase;

import com.fiap.techchallenge.pagamento.domain.Pagamento;

import java.util.Optional;

public interface PagamentoGateway {

    Optional<Pagamento> findUltimoByOsId(Long osId);

    Optional<Pagamento> findByIdTransacaoProvedor(String idTransacaoProvedor);

    Pagamento save(Pagamento pagamento);
}
