package br.com.accenture.payment.domain.model;

import br.com.accenture.payment.domain.enums.payment.PaymentMethod;
import br.com.accenture.payment.domain.enums.payment.PaymentStatus;
import br.com.accenture.payment.domain.exception.payment.InvalidPaymentStatusException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class Payment {

    private UUID id;
    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String externalTransactionId;
    private String failureReason;
    private Instant paidAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    private Payment(UUID id,
                    UUID orderId,
                    UUID customerId,
                    BigDecimal amount,
                    PaymentMethod method,
                    PaymentStatus status,
                    String externalTransactionId,
                    String failureReason,
                    Instant paidAt,
                    Instant createdAt,
                    Instant updatedAt,
                    Long version) {
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
        this.version = version;
    }

    public static Payment createNew(UUID orderId,
                                    UUID customerId,
                                    BigDecimal amount,
                                    PaymentMethod method) {
        requireNotNull(orderId, "orderId");
        requireNotNull(customerId, "customerId");
        requirePositive(amount);
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
                null,
                null
        );
    }

    public static Payment restore(UUID id,
                                  UUID orderId,
                                  UUID customerId,
                                  BigDecimal amount,
                                  PaymentMethod method,
                                  PaymentStatus status,
                                  String externalTransactionId,
                                  String failureReason,
                                  Instant paidAt,
                                  Instant createdAt,
                                  Instant updatedAt,
                                  Long version) {
        requireNotNull(id, "id");
        requireNotNull(orderId, "orderId");
        requireNotNull(customerId, "customerId");
        requirePositive(amount);
        requireNotNull(method, "method");
        requireNotNull(status, "status");

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
                updatedAt,
                version
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
        validatePendingOrProcessing("refuse");

        this.status = PaymentStatus.REFUSED;
        this.failureReason = failureReason;
        this.paidAt = null;
    }

    public void cancel(String failureReason) {
        validatePendingOrProcessing("cancel");

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
            throw new InvalidPaymentStatusException(this.status, action);
        }
    }

    private void validatePendingOrProcessing(String action) {
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.PROCESSING) {
            throw new InvalidPaymentStatusException(this.status, action);
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

    private static void requirePositive(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }
}