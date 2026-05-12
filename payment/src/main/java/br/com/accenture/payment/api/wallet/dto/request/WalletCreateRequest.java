package br.com.accenture.payment.api.wallet.dto.request;

import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WalletCreateRequest(

        @NotNull(message = "Owner ID is required")
        UUID ownerId,

        @NotNull(message = "Owner type is required")
        WalletOwnerType ownerType
) {
}