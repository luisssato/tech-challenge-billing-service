package com.fiap.techchallenge.pagamento.adapter.gateway.mercadopago;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record MercadoPagoPaymentRequest(
        @JsonProperty("transaction_amount") BigDecimal transactionAmount,
        String description,
        @JsonProperty("payment_method_id") String paymentMethodId,
        Payer payer,
        @JsonProperty("external_reference") String externalReference
) {
    public record Payer(String email) {}

    public static MercadoPagoPaymentRequest pix(BigDecimal valor, String descricao, String externalRef) {
        return new MercadoPagoPaymentRequest(valor, descricao, "pix", new Payer("cliente@oficina.com"), externalRef);
    }
}
