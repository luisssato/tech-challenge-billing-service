package com.fiap.techchallenge.pagamento.usecase;

import java.math.BigDecimal;

public record ConsultaPagamento(String idTransacao, StatusPagamentoProvedor status, BigDecimal valor) {
}
