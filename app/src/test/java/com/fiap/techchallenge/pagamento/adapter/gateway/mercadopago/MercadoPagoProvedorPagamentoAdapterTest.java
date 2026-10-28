package com.fiap.techchallenge.pagamento.adapter.gateway.mercadopago;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.pagamento.usecase.CobrancaQrCode;
import com.fiap.techchallenge.pagamento.usecase.ConsultaPagamento;
import com.fiap.techchallenge.pagamento.usecase.StatusPagamentoProvedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoProvedorPagamentoAdapterTest {

    private MockRestServiceServer mockServer;
    private MercadoPagoProvedorPagamentoAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        adapter = new MercadoPagoProvedorPagamentoAdapter("https://api.mercadopago.com", "TEST-TOKEN", restClient);
    }

    @Test
    void criaCobrancaComSucesso() {
        String jsonResponse = """
                {
                  "id": 987654321,
                  "status": "pending",
                  "status_detail": "waiting_transfer",
                  "transaction_amount": 450.00,
                  "point_of_interaction": {
                    "transaction_data": {
                      "qr_code": "00020126...pix-copia-e-cola",
                      "qr_code_base64": "iVBORw0KGgo..."
                    }
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer TEST-TOKEN"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        CobrancaQrCode cobranca = adapter.criarCobranca(10L, "orc-1", new BigDecimal("450.00"), "OS 10");

        assertThat(cobranca.idTransacao()).isEqualTo("987654321");
        assertThat(cobranca.qrCode()).isEqualTo("00020126...pix-copia-e-cola");
        assertThat(cobranca.qrCodeBase64()).isEqualTo("iVBORw0KGgo...");
        mockServer.verify();
    }

    @Test
    void criaCobrancaComFalhaLancaBadRequestException() {
        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThrows(BadRequestException.class, () ->
                adapter.criarCobranca(10L, "orc-1", new BigDecimal("450.00"), "OS 10"));
        mockServer.verify();
    }

    @Test
    void consultaPagamentoAprovado() {
        String jsonResponse = """
                {
                  "id": 987654321,
                  "status": "approved",
                  "transaction_amount": 450.00
                }
                """;

        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments/987654321"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer TEST-TOKEN"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ConsultaPagamento consulta = adapter.consultarPagamento("987654321");

        assertThat(consulta.status()).isEqualTo(StatusPagamentoProvedor.APROVADO);
        assertThat(consulta.valor()).isEqualByComparingTo("450.00");
        mockServer.verify();
    }

    @Test
    void consultaPagamentoRecusado() {
        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments/111"))
                .andRespond(withSuccess("{\"id\": 111, \"status\": \"rejected\", \"transaction_amount\": 10.0}", MediaType.APPLICATION_JSON));
        assertThat(adapter.consultarPagamento("111").status()).isEqualTo(StatusPagamentoProvedor.RECUSADO);
    }

    @Test
    void consultaPagamentoCancelado() {
        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments/222"))
                .andRespond(withSuccess("{\"id\": 222, \"status\": \"cancelled\", \"transaction_amount\": 10.0}", MediaType.APPLICATION_JSON));
        assertThat(adapter.consultarPagamento("222").status()).isEqualTo(StatusPagamentoProvedor.CANCELADO);
    }

    @Test
    void consultaPagamentoPendente() {
        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments/333"))
                .andRespond(withSuccess("{\"id\": 333, \"status\": \"in_process\", \"transaction_amount\": 10.0}", MediaType.APPLICATION_JSON));
        assertThat(adapter.consultarPagamento("333").status()).isEqualTo(StatusPagamentoProvedor.PENDENTE);
    }

    @Test
    void consultaPagamentoDesconhecidoOuOutro() {
        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments/444"))
                .andRespond(withSuccess("{\"id\": 444, \"status\": \"other\", \"transaction_amount\": 10.0}", MediaType.APPLICATION_JSON));
        assertThat(adapter.consultarPagamento("444").status()).isEqualTo(StatusPagamentoProvedor.DESCONHECIDO);
    }

    @Test
    void consultaPagamentoComErroRetornaDesconhecido() {
        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments/999"))
                .andRespond(withServerError());

        ConsultaPagamento consulta = adapter.consultarPagamento("999");
        assertThat(consulta.status()).isEqualTo(StatusPagamentoProvedor.DESCONHECIDO);
    }
}
