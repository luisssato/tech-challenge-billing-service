package com.fiap.techchallenge.pagamento.adapter.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MercadoPagoWebhookNotification {

    private String action;
    private String type;
    private WebhookData data;
    private String id;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookData {
        private String id;
    }

    public String extrairIdTransacao() {
        if (data != null && data.getId() != null && !data.getId().isBlank()) {
            return data.getId();
        }
        if (id != null && !id.isBlank()) {
            return id;
        }
        return null;
    }
}
