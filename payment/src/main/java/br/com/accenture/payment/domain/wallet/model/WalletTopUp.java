package br.com.accenture.payment.domain.wallet.model;

import br.com.accenture.payment.domain.wallet.enums.WalletTopUpStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class WalletTopUp {

    private final UUID id;
    private final UUID walletId;
    private final UUID customerId;
    private final BigDecimal amount;
    private WalletTopUpStatus status;
    private String externalOrderId;
    private String clientToken;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant creditedAt;

    private WalletTopUp(
            UUID id,
            UUID walletId,
            UUID customerId,
            BigDecimal amount,
            WalletTopUpStatus status,
            String externalOrderId,
            String clientToken,
            Instant createdAt,
            Instant updatedAt,
            Instant creditedAt
    ) {
        this.id = id;
        this.walletId = walletId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
        this.externalOrderId = externalOrderId;
        this.clientToken = clientToken;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.creditedAt = creditedAt;
    }

    public static WalletTopUp createNew(UUID walletId, UUID customerId, BigDecimal amount) {
        validateRequired(walletId, "Wallet id is required");
        validateRequired(customerId, "Customer id is required");
        validateAmount(amount);

        Instant now = Instant.now();

        return new WalletTopUp(
                UUID.randomUUID(),
                walletId,
                customerId,
                amount,
                WalletTopUpStatus.PENDING,
                null,
                null,
                now,
                now,
                null
        );
    }

    public static WalletTopUp restore(
            UUID id,
            UUID walletId,
            UUID customerId,
            BigDecimal amount,
            WalletTopUpStatus status,
            String externalOrderId,
            String clientToken,
            Instant createdAt,
            Instant updatedAt,
            Instant creditedAt
    ) {
        return new WalletTopUp(
                id,
                walletId,
                customerId,
                amount,
                status,
                externalOrderId,
                clientToken,
                createdAt,
                updatedAt,
                creditedAt
        );
    }

    public void attachExternalOrder(String externalOrderId, String clientToken) {
        if (externalOrderId == null || externalOrderId.isBlank()) {
            throw new IllegalArgumentException("External order id is required");
        }

        if (clientToken == null || clientToken.isBlank()) {
            throw new IllegalArgumentException("Client token is required");
        }

        this.externalOrderId = externalOrderId;
        this.clientToken = clientToken;
        this.updatedAt = Instant.now();
    }

    public void approve() {
        if (status == WalletTopUpStatus.APPROVED) {
            return;
        }

        this.status = WalletTopUpStatus.APPROVED;
        this.creditedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void refuse() {
        this.status = WalletTopUpStatus.REFUSED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = WalletTopUpStatus.CANCELED;
        this.updatedAt = Instant.now();
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top up amount must be greater than zero");
        }
    }

    private static void validateRequired(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
}