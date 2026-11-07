package com.fiap.techchallenge.config;

import com.fiap.techchallenge.pagamento.adapter.gateway.MockProvedorPagamentoAdapter;
import com.fiap.techchallenge.pagamento.adapter.gateway.mercadopago.MercadoPagoProvedorPagamentoAdapter;
import com.fiap.techchallenge.pagamento.usecase.ProvedorPagamentoGateway;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class PagamentoConfigTest {

    private final PagamentoConfig config = new PagamentoConfig();
    private final MockProvedorPagamentoAdapter mockAdapter = new MockProvedorPagamentoAdapter();
    private final RestClient restClient = RestClient.builder().build();

    @Test
    void criaRestClientBean() {
        assertThat(config.restClient()).isNotNull();
    }

    @Test
    void selecionaMockQuandoProvedorForMock() {
        ProvedorPagamentoGateway gateway = config.provedorPagamentoGateway("mock", "https://api.mercadopago.com", "", restClient, mockAdapter);
        assertThat(gateway).isSameAs(mockAdapter);
    }

    @Test
    void selecionaMockQuandoMercadoPagoSemToken() {
        ProvedorPagamentoGateway gateway = config.provedorPagamentoGateway("mercadopago", "https://api.mercadopago.com", "", restClient, mockAdapter);
        assertThat(gateway).isSameAs(mockAdapter);
    }

    @Test
    void selecionaMercadoPagoQuandoConfiguradoComToken() {
        ProvedorPagamentoGateway gateway = config.provedorPagamentoGateway("mercadopago", "https://api.mercadopago.com", "TOKEN", restClient, mockAdapter);
        assertThat(gateway).isInstanceOf(MercadoPagoProvedorPagamentoAdapter.class);
    }
}
