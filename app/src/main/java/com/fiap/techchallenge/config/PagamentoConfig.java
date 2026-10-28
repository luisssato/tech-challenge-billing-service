package com.fiap.techchallenge.config;

import com.fiap.techchallenge.pagamento.adapter.gateway.MockProvedorPagamentoAdapter;
import com.fiap.techchallenge.pagamento.adapter.gateway.mercadopago.MercadoPagoProvedorPagamentoAdapter;
import com.fiap.techchallenge.pagamento.usecase.ProvedorPagamentoGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class PagamentoConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

    @Bean
    @Primary
    public ProvedorPagamentoGateway provedorPagamentoGateway(
            @Value("${pagamento.provedor:mock}") String provedor,
            @Value("${mercadopago.base-url:https://api.mercadopago.com}") String mpBaseUrl,
            @Value("${mercadopago.access-token:}") String mpAccessToken,
            RestClient restClient,
            MockProvedorPagamentoAdapter mockAdapter) {

        if ("mercadopago".equalsIgnoreCase(provedor) && mpAccessToken != null && !mpAccessToken.isBlank()) {
            log.info("[PAGAMENTO] Ativando provedor oficial MERCADO PAGO na URL {}", mpBaseUrl);
            return new MercadoPagoProvedorPagamentoAdapter(mpBaseUrl, mpAccessToken, restClient);
        }

        log.info("[PAGAMENTO] Ativando provedor MOCK/SIMULADOR de pagamentos");
        return mockAdapter;
    }
}
