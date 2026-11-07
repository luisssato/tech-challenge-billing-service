package com.fiap.techchallenge.pagamento.usecase;

public record CobrancaQrCode(String idTransacao, String qrCode, String qrCodeBase64) {
}
