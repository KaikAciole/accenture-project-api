package br.com.accenture.payment.support;

import br.com.accenture.payment.domain.enums.payment.PaymentMethod;
import br.com.accenture.payment.domain.enums.payment.PaymentStatus;
import br.com.accenture.payment.domain.model.Payment;

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
}
