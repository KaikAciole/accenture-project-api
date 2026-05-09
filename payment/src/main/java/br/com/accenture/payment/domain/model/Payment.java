package br.com.accenture.payment.domain.model;

import br.com.accenture.payment.domain.enums.PaymentMethod;
import br.com.accenture.payment.domain.enums.PaymentStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class Payment {

    private UUID id;
    private UUID orderId;
    private String customerId;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String externalTransactionId;
    private String failureReason;
    private Instant paidAt;
    private Instant createdAt;
    private Instant updatedAt;

    private Payment(UUID id,
                    UUID orderId,
                    String customerId,
                    BigDecimal amount,
                    PaymentMethod method,
                    PaymentStatus status,
                    String externalTransactionId,
                    String failureReason,
                    Instant paidAt,
                    Instant createdAt,
                    Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.externalTransactionId = externalTransactionId;
        this.failureReason = failureReason;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment createNew(UUID orderId,
                                    String customerId,
                                    BigDecimal amount,
                                    PaymentMethod method) {
        requireNotNull(orderId, "orderId");
        requireNotBlank(customerId, "customerId");
        requirePositive(amount, "amount");
        requireNotNull(method, "method");

        return new Payment(
                null,
                orderId,
                customerId,
                amount,
                method,
                PaymentStatus.PENDING,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static Payment restore(UUID id,
                                  UUID orderId,
                                  String customerId,
                                  BigDecimal amount,
                                  PaymentMethod method,
                                  PaymentStatus status,
                                  String externalTransactionId,
                                  String failureReason,
                                  Instant paidAt,
                                  Instant createdAt,
                                  Instant updatedAt) {
        return new Payment(
                id,
                orderId,
                customerId,
                amount,
                method,
                status,
                externalTransactionId,
                failureReason,
                paidAt,
                createdAt,
                updatedAt
        );
    }

    public void process(String externalTransactionId) {
        requireNotBlank(externalTransactionId, "externalTransactionId");
        validateStatus(PaymentStatus.PENDING, "process");

        this.externalTransactionId = externalTransactionId;
        this.status = PaymentStatus.PROCESSING;
        this.failureReason = null;
    }

    public void approve() {
        validateStatus(PaymentStatus.PROCESSING, "approve");

        this.status = PaymentStatus.APPROVED;
        this.failureReason = null;
        this.paidAt = Instant.now();
    }

    public void refuse(String failureReason) {
        requireNotBlank(failureReason, "failureReason");

        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Cannot refuse payment from current state: " + this.status);
        }

        this.status = PaymentStatus.REFUSED;
        this.failureReason = failureReason;
        this.paidAt = null;
    }

    public void cancel(String failureReason) {
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Cannot cancel payment from current state: " + this.status);
        }

        this.status = PaymentStatus.CANCELED;
        this.failureReason = failureReason;
        this.paidAt = null;
    }

    public void refund() {
        validateStatus(PaymentStatus.APPROVED, "refund");

        this.status = PaymentStatus.REFUNDED;
    }

    private void validateStatus(PaymentStatus expectedStatus, String action) {
        if (this.status != expectedStatus) {
            throw new IllegalStateException("Cannot " + action + " payment from current state: " + this.status);
        }
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private static void requireNotNull(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }
}
