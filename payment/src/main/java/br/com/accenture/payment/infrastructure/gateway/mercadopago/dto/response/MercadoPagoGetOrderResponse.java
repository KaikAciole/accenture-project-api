package br.com.accenture.payment.infrastructure.gateway.mercadopago.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record MercadoPagoGetOrderResponse(
        String id,

        @JsonProperty("external_reference")
        String externalReference,

        String status,

        @JsonProperty("status_detail")
        String statusDetail,

        @JsonProperty("total_amount")
        BigDecimal totalAmount,

        @JsonProperty("total_paid_amount")
        BigDecimal totalPaidAmount
) {
}