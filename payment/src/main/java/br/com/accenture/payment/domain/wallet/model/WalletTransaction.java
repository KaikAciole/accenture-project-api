package br.com.accenture.payment.domain.wallet.model;

import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionType;
import br.com.accenture.payment.domain.wallet.exception.InvalidWalletTransactionException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class WalletTransaction {

    private final UUID id;
    private final UUID walletId;
    private final UUID paymentId;
    private final WalletTransactionType type;
    private final WalletTransactionReason reason;
    private final BigDecimal amount;
    private final Instant createdAt;

    private WalletTransaction(
            UUID id,
            UUID walletId,
            UUID paymentId,
            WalletTransactionType type,
            WalletTransactionReason reason,
            BigDecimal amount,
            Instant createdAt
    ) {
        validate(walletId, type, reason, amount);

        this.id = id;
        this.walletId = walletId;
        this.paymentId = paymentId;
        this.type = type;
        this.reason = reason;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public static WalletTransaction credit(
            UUID walletId,
            UUID paymentId,
            BigDecimal amount,
            WalletTransactionReason reason
    ) {
        return new WalletTransaction(
                null,
                walletId,
                paymentId,
                WalletTransactionType.CREDIT,
                reason,
                amount,
                Instant.now()
        );
    }

    public static WalletTransaction debit(
            UUID walletId,
            UUID paymentId,
            BigDecimal amount,
            WalletTransactionReason reason
    ) {
        return new WalletTransaction(
                null,
                walletId,
                paymentId,
                WalletTransactionType.DEBIT,
                reason,
                amount,
                Instant.now()
        );
    }

    public static WalletTransaction restore(
            UUID id,
            UUID walletId,
            UUID paymentId,
            WalletTransactionType type,
            WalletTransactionReason reason,
            BigDecimal amount,
            Instant createdAt
    ) {
        return new WalletTransaction(
                id,
                walletId,
                paymentId,
                type,
                reason,
                amount,
                createdAt
        );
    }

    private static void validate(
            UUID walletId,
            WalletTransactionType type,
            WalletTransactionReason reason,
            BigDecimal amount
    ) {
        if (walletId == null) {
            throw new InvalidWalletTransactionException("Wallet id is required");
        }

        if (type == null) {
            throw new InvalidWalletTransactionException("Wallet transaction type is required");
        }

        if (reason == null) {
            throw new InvalidWalletTransactionException("Wallet transaction reason is required");
        }

        if (amount == null) {
            throw new InvalidWalletTransactionException("Amount is required");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidWalletTransactionException("Amount must be greater than zero");
        }
    }
}