package br.com.accenture.payment.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentProcessRequest(

        @NotBlank(message = "External transaction ID is required")
        String externalTransactionId
) {
}