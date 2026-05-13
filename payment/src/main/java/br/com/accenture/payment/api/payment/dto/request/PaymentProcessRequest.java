package br.com.accenture.payment.api.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentProcessRequest(

        @Schema(
                description = "External transaction identifier. Required for external payment methods such as PIX, CREDIT_CARD and DEBIT_CARD. For WALLET payments, this value is generated internally.",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        String externalTransactionId
) {
}