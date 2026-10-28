package com.fiap.techchallenge.pagamento.adapter.gateway;

import com.fiap.techchallenge.pagamento.usecase.CobrancaQrCode;
import com.fiap.techchallenge.pagamento.usecase.ConsultaPagamento;
import com.fiap.techchallenge.pagamento.usecase.StatusPagamentoProvedor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MockProvedorPagamentoAdapterTest {

    private final MockProvedorPagamentoAdapter adapter = new MockProvedorPagamentoAdapter();

    @Test
    void criaCobrancaPixMockadaComSucesso() {
        CobrancaQrCode cobranca = adapter.criarCobranca(10L, "orc-1", new BigDecimal("400.00"), "OS 10");

        assertThat(cobranca.idTransacao()).startsWith("mock-mp-");
        assertThat(cobranca.qrCode()).contains(cobranca.idTransacao());
        assertThat(cobranca.qrCodeBase64()).startsWith("data:image/png;base64,");

        ConsultaPagamento consulta = adapter.consultarPagamento(cobranca.idTransacao());
        assertThat(consulta.status()).isEqualTo(StatusPagamentoProvedor.APROVADO);
        assertThat(consulta.valor()).isEqualByComparingTo("400.00");
    }

    @Test
    void permiteDefinirStatusParaSimulacao() {
        CobrancaQrCode cobranca = adapter.criarCobranca(10L, "orc-1", new BigDecimal("100.00"), "OS 10");
        adapter.definirStatus(cobranca.idTransacao(), StatusPagamentoProvedor.RECUSADO);

        ConsultaPagamento consulta = adapter.consultarPagamento(cobranca.idTransacao());
        assertThat(consulta.status()).isEqualTo(StatusPagamentoProvedor.RECUSADO);
    }

    @Test
    void consultaTransacaoInexistenteComRecusaRetornaRecusado() {
        ConsultaPagamento consulta = adapter.consultarPagamento("transacao-recusada-123");
        assertThat(consulta.status()).isEqualTo(StatusPagamentoProvedor.RECUSADO);
        assertThat(consulta.valor()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
