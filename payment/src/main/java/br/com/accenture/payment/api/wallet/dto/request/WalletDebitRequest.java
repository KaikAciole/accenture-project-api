package br.com.accenture.payment.api.wallet.dto.request;

import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletDebitRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Reason is required")
        WalletTransactionReason reason,

        UUID paymentId
) {
}