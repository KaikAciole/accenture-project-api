package br.com.accenture.payment.api.wallet.dto.response;

import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id,
        UUID walletId,
        UUID paymentId,
        WalletTransactionType type,
        WalletTransactionReason reason,
        BigDecimal amount,
        Instant createdAt
) {
}