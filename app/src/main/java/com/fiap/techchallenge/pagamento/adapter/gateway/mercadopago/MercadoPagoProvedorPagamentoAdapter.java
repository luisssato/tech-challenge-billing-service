package com.fiap.techchallenge.pagamento.adapter.gateway.mercadopago;

import com.fiap.techchallenge.exception.BadRequestException;
import com.fiap.techchallenge.pagamento.usecase.CobrancaQrCode;
import com.fiap.techchallenge.pagamento.usecase.ConsultaPagamento;
import com.fiap.techchallenge.pagamento.usecase.ProvedorPagamentoGateway;
import com.fiap.techchallenge.pagamento.usecase.StatusPagamentoProvedor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
public class MercadoPagoProvedorPagamentoAdapter implements ProvedorPagamentoGateway {

    private final String baseUrl;
    private final String accessToken;
    private final RestClient restClient;

    public MercadoPagoProvedorPagamentoAdapter(String baseUrl, String accessToken, RestClient restClient) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.accessToken = accessToken;
        this.restClient = restClient;
    }

    @Override
    public CobrancaQrCode criarCobranca(Long osId, String orcamentoId, BigDecimal valor, String descricao) {
        String externalRef = String.valueOf(osId);
        MercadoPagoPaymentRequest request = MercadoPagoPaymentRequest.pix(valor, descricao, externalRef);
        String idempotencyKey = UUID.randomUUID().toString();

        log.info("[MERCADO PAGO] Criando cobrança Pix para OS {}, valor {}", osId, valor);
        try {
            MercadoPagoPaymentResponse response = restClient.post()
                    .uri(baseUrl + "/v1/payments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(MercadoPagoPaymentResponse.class);

            if (response == null || response.id() == null) {
                throw new BadRequestException("Erro: Provedor de pagamento não retornou identificador válido para a OS " + osId);
            }

            String idTransacao = String.valueOf(response.id());
            String qrCode = null;
            String qrCodeBase64 = null;
            if (response.pointOfInteraction() != null && response.pointOfInteraction().transactionData() != null) {
                qrCode = response.pointOfInteraction().transactionData().qrCode();
                qrCodeBase64 = response.pointOfInteraction().transactionData().qrCodeBase64();
            }

            log.info("[MERCADO PAGO] Cobrança Pix criada com sucesso: idTransacao={}", idTransacao);
            return new CobrancaQrCode(idTransacao, qrCode, qrCodeBase64);
        } catch (Exception e) {
            log.error("[MERCADO PAGO] Falha ao criar cobrança Pix para OS {}: {}", osId, e.getMessage(), e);
            throw new BadRequestException("Erro ao comunicar com Mercado Pago: " + e.getMessage());
        }
    }

    @Override
    public ConsultaPagamento consultarPagamento(String idTransacao) {
        log.info("[MERCADO PAGO] Consultando transação {}", idTransacao);
        try {
            MercadoPagoPaymentResponse response = restClient.get()
                    .uri(baseUrl + "/v1/payments/" + idTransacao)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(MercadoPagoPaymentResponse.class);

            if (response == null) {
                return new ConsultaPagamento(idTransacao, StatusPagamentoProvedor.DESCONHECIDO, BigDecimal.ZERO);
            }

            StatusPagamentoProvedor status = mapStatus(response.status());
            BigDecimal valor = response.transactionAmount() != null ? response.transactionAmount() : BigDecimal.ZERO;
            return new ConsultaPagamento(idTransacao, status, valor);
        } catch (Exception e) {
            log.error("[MERCADO PAGO] Falha ao consultar transação {}: {}", idTransacao, e.getMessage(), e);
            return new ConsultaPagamento(idTransacao, StatusPagamentoProvedor.DESCONHECIDO, BigDecimal.ZERO);
        }
    }

    private StatusPagamentoProvedor mapStatus(String mpStatus) {
        if (mpStatus == null) return StatusPagamentoProvedor.DESCONHECIDO;
        return switch (mpStatus.toLowerCase()) {
            case "approved" -> StatusPagamentoProvedor.APROVADO;
            case "rejected" -> StatusPagamentoProvedor.RECUSADO;
            case "cancelled" -> StatusPagamentoProvedor.CANCELADO;
            case "pending", "in_process", "authorized" -> StatusPagamentoProvedor.PENDENTE;
            default -> StatusPagamentoProvedor.DESCONHECIDO;
        };
    }
}
