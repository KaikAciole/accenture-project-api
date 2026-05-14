package br.com.accenture.payment.infrastructure.gateway.mercadopago.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MercadoPagoCreateOrderRequest(
        String type,

        @JsonProperty("processing_mode")
        String processingMode,

        @JsonProperty("external_reference")
        String externalReference,

        @JsonProperty("total_amount")
        String totalAmount,

        Payer payer,

        List<Item> items
) {
    public record Payer(
            String email
    ) {
    }

    public record Item(
            String title,
            Integer quantity,

            @JsonProperty("unit_price")
            String unitPrice
    ) {
    }
}