package br.com.accenture.payment.support;

import br.com.accenture.payment.domain.payment.enums.PaymentMethod;
import br.com.accenture.payment.domain.payment.enums.PaymentStatus;
import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionType;
import br.com.accenture.payment.domain.wallet.model.Wallet;
import br.com.accenture.payment.domain.wallet.model.WalletTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TestFixtures {

    public static final UUID PAYMENT_ID = UUID.fromString("6ca24443-0347-486b-b276-290f4170909f");
    public static final UUID ORDER_ID = UUID.fromString("e3bc2c53-e29c-4a19-9063-8b8cb55507d6");
    public static final UUID CUSTOMER_ID = UUID.fromString("2a497a58-b4e5-44ac-a79b-797ca294865e");
    public static final BigDecimal AMOUNT = new BigDecimal("149.90");
    public static final String EXTERNAL_TRANSACTION_ID = "tx-123";
    public static final String FAILURE_REASON = "Card declined";
    public static final Instant CREATED_AT = Instant.parse("2026-05-09T10:00:00Z");
    public static final Instant UPDATED_AT = Instant.parse("2026-05-09T10:05:00Z");
    public static final Instant PAID_AT = Instant.parse("2026-05-09T10:10:00Z");
    public static final UUID WALLET_ID = UUID.fromString("764c9389-8d30-42e1-9f5d-b07a109445f7");
    public static final UUID SELLER_WALLET_ID = UUID.fromString("bd2fbc0f-0e60-478e-9e12-f40e5ef4cdb5");
    public static final UUID OWNER_ID = UUID.fromString("751408b4-388e-4e3a-ac8e-c5f3e1e906d2");
    public static final UUID SELLER_ID = UUID.fromString("9dd9cfa2-3412-4d6e-9f23-27e543c0d845");
    public static final UUID WALLET_TRANSACTION_ID = UUID.fromString("9d1206cd-e5f8-4886-87a4-c0fc072c4c88");
    public static final BigDecimal WALLET_BALANCE = new BigDecimal("250.00");

    private TestFixtures() {
    }

    public static Payment newPayment() {
        return Payment.createNew(ORDER_ID, CUSTOMER_ID, AMOUNT, PaymentMethod.PIX);
    }

    public static Payment pendingPayment() {
        return restoredPayment(PaymentStatus.PENDING, null, null, null);
    }

    public static Payment processingPayment() {
        return restoredPayment(PaymentStatus.PROCESSING, EXTERNAL_TRANSACTION_ID, null, null);
    }

    public static Payment approvedPayment() {
        return restoredPayment(PaymentStatus.APPROVED, EXTERNAL_TRANSACTION_ID, null, PAID_AT);
    }

    public static Payment refusedPayment() {
        return restoredPayment(PaymentStatus.REFUSED, EXTERNAL_TRANSACTION_ID, FAILURE_REASON, null);
    }

    public static Payment canceledPayment() {
        return restoredPayment(PaymentStatus.CANCELED, null, "Customer requested", null);
    }

    public static Payment refundedPayment() {
        return restoredPayment(PaymentStatus.REFUNDED, EXTERNAL_TRANSACTION_ID, null, PAID_AT);
    }

    public static Payment restoredPayment(PaymentStatus status,
                                           String externalTransactionId,
                                           String failureReason,
                                           Instant paidAt) {
        return Payment.restore(
                PAYMENT_ID,
                ORDER_ID,
                CUSTOMER_ID,
                AMOUNT,
                PaymentMethod.PIX,
                status,
                externalTransactionId,
                failureReason,
                paidAt,
                CREATED_AT,
                UPDATED_AT,
                1L
        );
    }

    public static Wallet newWallet() {
        return Wallet.createNew(OWNER_ID, WalletOwnerType.COSTUMER);
    }

    public static Wallet walletWithBalance() {
        return walletWithBalance(WALLET_ID, OWNER_ID, WalletOwnerType.COSTUMER, WALLET_BALANCE);
    }

    public static Wallet sellerWalletWithBalance() {
        return walletWithBalance(SELLER_WALLET_ID, SELLER_ID, WalletOwnerType.SELLER, BigDecimal.ZERO);
    }

    public static Wallet walletWithBalance(UUID id,
                                           UUID ownerId,
                                           WalletOwnerType ownerType,
                                           BigDecimal balance) {
        return Wallet.restore(
                id,
                ownerId,
                ownerType,
                balance,
                CREATED_AT,
                UPDATED_AT,
                1L
        );
    }

    public static WalletTransaction walletCreditTransaction() {
        return WalletTransaction.restore(
                WALLET_TRANSACTION_ID,
                WALLET_ID,
                PAYMENT_ID,
                WalletTransactionType.CREDIT,
                WalletTransactionReason.TOP_UP,
                AMOUNT,
                CREATED_AT
        );
    }

    public static WalletTransaction walletDebitTransaction() {
        return WalletTransaction.restore(
                WALLET_TRANSACTION_ID,
                WALLET_ID,
                PAYMENT_ID,
                WalletTransactionType.DEBIT,
                WalletTransactionReason.PAYMENT,
                AMOUNT,
                CREATED_AT
        );
    }
}
