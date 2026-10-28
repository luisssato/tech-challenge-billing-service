package com.fiap.techchallenge.pagamento.usecase;

import java.math.BigDecimal;

public interface ProvedorPagamentoGateway {

    CobrancaQrCode criarCobranca(Long osId, String orcamentoId, BigDecimal valor, String descricao);

    ConsultaPagamento consultarPagamento(String idTransacao);
}
