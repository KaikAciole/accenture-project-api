package br.com.accenture.payment.api.wallet.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletTopUpResponse(
        UUID id,
        UUID walletId,
        UUID customerId,
        BigDecimal amount,
        String status
) {
}