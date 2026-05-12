package br.com.accenture.payment.api.wallet.dto.response;

import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID ownerId,
        WalletOwnerType ownerType,
        BigDecimal balance,
        Instant createdAt,
        Instant updatedAt
) {
}