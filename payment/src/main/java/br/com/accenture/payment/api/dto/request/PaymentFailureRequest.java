package br.com.accenture.payment.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentFailureRequest(

        @NotBlank(message = "Failure reason is required")
        String failureReason
) {
}