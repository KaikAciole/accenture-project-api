package br.com.accenture.payment.api.wallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletTopUpRequest(
        @NotNull
        UUID customerId,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        @NotNull
        @Email
        String customerEmail
) {
}