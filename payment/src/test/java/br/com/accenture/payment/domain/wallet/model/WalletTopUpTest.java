package br.com.accenture.payment.domain.wallet.model;

import br.com.accenture.payment.domain.wallet.enums.WalletTopUpStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class WalletTopUpTest {

    private static final UUID WALLET_ID = UUID.fromString("8f1e9c14-2c32-4d2c-8c7c-09e6a8d0d3a7");
    private static final UUID CUSTOMER_ID = UUID.fromString("8d4c1f6e-9a93-4e88-83c4-7cf38c4a9b21");
    private static final BigDecimal AMOUNT = new BigDecimal("80.00");

    @Test
    void createNewBuildsPendingTopUpWithIdAndTimestamps() {
        Instant before = Instant.now();
        WalletTopUp topUp = WalletTopUp.createNew(WALLET_ID, CUSTOMER_ID, AMOUNT);
        Instant after = Instant.now();

        assertThat(topUp.getId()).isNotNull();
        assertThat(topUp.getWalletId()).isEqualTo(WALLET_ID);
        assertThat(topUp.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(topUp.getAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(topUp.getStatus()).isEqualTo(WalletTopUpStatus.PENDING);
        assertThat(topUp.getExternalOrderId()).isNull();
        assertThat(topUp.getClientToken()).isNull();
        assertThat(topUp.getCreatedAt()).isBetween(before, after);
        assertThat(topUp.getUpdatedAt()).isEqualTo(topUp.getCreatedAt());
        assertThat(topUp.getCreditedAt()).isNull();
    }

    @Test
    void restorePreservesAllFields() {
        UUID id = UUID.fromString("0e8f70cb-6c30-4f0e-9e1c-72d2c5b3a3a1");
        Instant createdAt = Instant.parse("2026-05-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-05-01T10:30:00Z");
        Instant creditedAt = Instant.parse("2026-05-01T10:31:00Z");

        WalletTopUp topUp = WalletTopUp.restore(
                id,
                WALLET_ID,
                CUSTOMER_ID,
                AMOUNT,
                WalletTopUpStatus.APPROVED,
                "ext-1",
                "token-1",
                createdAt,
                updatedAt,
                creditedAt
        );

        assertThat(topUp.getId()).isEqualTo(id);
        assertThat(topUp.getWalletId()).isEqualTo(WALLET_ID);
        assertThat(topUp.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(topUp.getAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(topUp.getStatus()).isEqualTo(WalletTopUpStatus.APPROVED);
        assertThat(topUp.getExternalOrderId()).isEqualTo("ext-1");
        assertThat(topUp.getClientToken()).isEqualTo("token-1");
        assertThat(topUp.getCreatedAt()).isEqualTo(createdAt);
        assertThat(topUp.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(topUp.getCreditedAt()).isEqualTo(creditedAt);
    }

    @Test
    void createNewRejectsNullWalletId() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> WalletTopUp.createNew(null, CUSTOMER_ID, AMOUNT))
                .withMessage("Wallet id is required");
    }

    @Test
    void createNewRejectsNullCustomerId() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> WalletTopUp.createNew(WALLET_ID, null, AMOUNT))
                .withMessage("Customer id is required");
    }

    @Test
    void createNewRejectsNullAmount() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> WalletTopUp.createNew(WALLET_ID, CUSTOMER_ID, null))
                .withMessage("Top up amount must be greater than zero");
    }

    @Test
    void createNewRejectsZeroAmount() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> WalletTopUp.createNew(WALLET_ID, CUSTOMER_ID, BigDecimal.ZERO))
                .withMessage("Top up amount must be greater than zero");
    }

    @Test
    void createNewRejectsNegativeAmount() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> WalletTopUp.createNew(WALLET_ID, CUSTOMER_ID, new BigDecimal("-1")))
                .withMessage("Top up amount must be greater than zero");
    }

    @Test
    void attachExternalOrderUpdatesIdentifiersAndTimestamp() throws InterruptedException {
        WalletTopUp topUp = WalletTopUp.createNew(WALLET_ID, CUSTOMER_ID, AMOUNT);
        Instant originalUpdatedAt = topUp.getUpdatedAt();
        Thread.sleep(2);

        topUp.attachExternalOrder("ext-99", "token-99");

        assertThat(topUp.getExternalOrderId()).isEqualTo("ext-99");
        assertThat(topUp.getClientToken()).isEqualTo("token-99");
        assertThat(topUp.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void attachExternalOrderRejectsNullExternalOrderId() {
        WalletTopUp topUp = WalletTopUp.createNew(WALLET_ID, CUSTOMER_ID, AMOUNT);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> topUp.attachExternalOrder(null, "token"))
                .withMessage("External order id is required");
    }

    @Test
    void attachExternalOrderRejectsBlankExternalOrderId() {
        WalletTopUp topUp = WalletTopUp.createNew(WALLET_ID, CUSTOMER_ID, AMOUNT);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> topUp.attachExternalOrder("  ", "token"))
                .withMessage("External order id is required");
    }

    @Test
    void approveTransitionsToApprovedAndSetsCreditedAt() {
        WalletTopUp topUp = WalletTopUp.createNew(WALLET_ID, CUSTOMER_ID, AMOUNT);

        topUp.approve();

        assertThat(topUp.getStatus()).isEqualTo(WalletTopUpStatus.APPROVED);
        assertThat(topUp.getCreditedAt()).isNotNull();
    }

    @Test
    void approveIsIdempotentWhenAlreadyApproved() {
        WalletTopUp topUp = WalletTopUp.restore(
                UUID.randomUUID(),
                WALLET_ID,
                CUSTOMER_ID,
                AMOUNT,
                WalletTopUpStatus.APPROVED,
                "ext-1",
                "token-1",
                Instant.parse("2026-05-01T10:00:00Z"),
                Instant.parse("2026-05-01T10:30:00Z"),
                Instant.parse("2026-05-01T10:31:00Z")
        );
        Instant initialCreditedAt = topUp.getCreditedAt();
        Instant initialUpdatedAt = topUp.getUpdatedAt();

        topUp.approve();

        assertThat(topUp.getStatus()).isEqualTo(WalletTopUpStatus.APPROVED);
        assertThat(topUp.getCreditedAt()).isEqualTo(initialCreditedAt);
        assertThat(topUp.getUpdatedAt()).isEqualTo(initialUpdatedAt);
    }

    @Test
    void refuseTransitionsToRefused() {
        WalletTopUp topUp = WalletTopUp.createNew(WALLET_ID, CUSTOMER_ID, AMOUNT);

        topUp.refuse();

        assertThat(topUp.getStatus()).isEqualTo(WalletTopUpStatus.REFUSED);
    }

    @Test
    void cancelTransitionsToCanceled() {
        WalletTopUp topUp = WalletTopUp.createNew(WALLET_ID, CUSTOMER_ID, AMOUNT);

        topUp.cancel();

        assertThat(topUp.getStatus()).isEqualTo(WalletTopUpStatus.CANCELED);
    }
}
