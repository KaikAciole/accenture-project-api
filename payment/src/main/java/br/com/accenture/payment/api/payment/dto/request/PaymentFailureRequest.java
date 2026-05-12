package br.com.accenture.payment.api.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentFailureRequest(

        @NotBlank(message = "Failure reason is required")
        String failureReason
) {
}