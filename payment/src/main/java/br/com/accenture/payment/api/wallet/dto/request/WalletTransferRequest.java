package br.com.accenture.payment.api.wallet.dto.request;

import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletTransferRequest(

        @NotNull(message = "Source owner ID is required")
        UUID fromOwnerId,

        @NotNull(message = "Source owner type is required")
        WalletOwnerType fromOwnerType,

        @NotNull(message = "Target owner ID is required")
        UUID toOwnerId,

        @NotNull(message = "Target owner type is required")
        WalletOwnerType toOwnerType,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        UUID paymentId
) {
}