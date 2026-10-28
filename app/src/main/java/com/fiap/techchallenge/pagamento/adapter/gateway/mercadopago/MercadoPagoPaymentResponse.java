package com.fiap.techchallenge.pagamento.adapter.gateway.mercadopago;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record MercadoPagoPaymentResponse(
        Long id,
        String status,
        @JsonProperty("status_detail") String statusDetail,
        @JsonProperty("transaction_amount") BigDecimal transactionAmount,
        @JsonProperty("point_of_interaction") PointOfInteraction pointOfInteraction
) {
    public record PointOfInteraction(
            @JsonProperty("transaction_data") TransactionData transactionData
    ) {}

    public record TransactionData(
            @JsonProperty("qr_code") String qrCode,
            @JsonProperty("qr_code_base64") String qrCodeBase64
    ) {}
}
