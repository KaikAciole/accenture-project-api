package br.com.accenture.payment.api.webhook.dto;

public record MercadoPagoWebhookRequest(
        String action,

        String type,

        MercadoPagoWebhookData data
) {
    public record MercadoPagoWebhookData(
            String id
    ) {
    }
}