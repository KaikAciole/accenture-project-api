package br.com.accenture.payment.infrastructure.gateway.mercadopago.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MercadoPagoCreateOrderResponse(
        String id,
        String status,

        @JsonProperty("client_token")
        String clientToken,

        @JsonProperty("total_amount")
        String totalAmount
) {
}