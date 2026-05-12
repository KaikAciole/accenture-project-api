package br.com.accenture.payment.infrastructure.persistence.payment.mapper;

import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.infrastructure.persistence.payment.entity.PaymentJpaEntity;

public final class PaymentPersistenceMapper {

    private PaymentPersistenceMapper() {
    }

    public static PaymentJpaEntity toEntity(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentJpaEntity.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .externalTransactionId(payment.getExternalTransactionId())
                .failureReason(payment.getFailureReason())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .version(payment.getVersion())
                .build();
    }

    public static Payment toDomain(PaymentJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Payment.restore(
                entity.getId(),
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getAmount(),
                entity.getMethod(),
                entity.getStatus(),
                entity.getExternalTransactionId(),
                entity.getFailureReason(),
                entity.getPaidAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}