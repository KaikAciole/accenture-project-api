package br.com.accenture.payment.domain.wallet.model;


import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.exception.InsufficientWalletBalanceException;
import br.com.accenture.payment.domain.wallet.exception.InvalidWalletTransactionException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class Wallet {

    private final UUID id;
    private final UUID ownerId;
    private final WalletOwnerType ownerType;
    private BigDecimal balance;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Long version;

    private Wallet(
            UUID id,
            UUID ownerId,
            WalletOwnerType ownerType,
            BigDecimal balance,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        validateOwner(ownerId, ownerType);
        validateBalance(balance);

        this.id = id;
        this.ownerId = ownerId;
        this.ownerType = ownerType;
        this.balance = balance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Wallet createNew(UUID ownerId, WalletOwnerType ownerType) {
        Instant now = Instant.now();

        return new Wallet(
                null,
                ownerId,
                ownerType,
                BigDecimal.ZERO,
                now,
                now,
                null
        );
    }

    public static Wallet restore(
            UUID id,
            UUID ownerId,
            WalletOwnerType ownerType,
            BigDecimal balance,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        return new Wallet(
                id,
                ownerId,
                ownerType,
                balance,
                createdAt,
                updatedAt,
                version
        );
    }

    public void credit(BigDecimal amount) {
        validatePositiveAmount(amount);

        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    public void debit(BigDecimal amount) {
        validatePositiveAmount(amount);

        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientWalletBalanceException(
                    "Insufficient wallet balance to complete this transaction"
            );
        }

        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    private static void validateOwner(UUID ownerId, WalletOwnerType ownerType) {
        if (ownerId == null) {
            throw new IllegalArgumentException("Wallet owner id is required");
        }

        if (ownerType == null) {
            throw new IllegalArgumentException("Wallet owner type is required");
        }
    }

    private static void validateBalance(BigDecimal balance) {
        if (balance == null) {
            throw new IllegalArgumentException("Wallet balance is required");
        }

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidWalletTransactionException("Wallet balance cannot be negative");
        }
    }

    private static void validatePositiveAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidWalletTransactionException("Amount is required");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidWalletTransactionException("Amount must be greater than zero");
        }
    }}