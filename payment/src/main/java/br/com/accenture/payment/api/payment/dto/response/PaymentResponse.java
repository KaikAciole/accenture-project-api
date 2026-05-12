package br.com.accenture.payment.api.payment.dto.response;

import br.com.accenture.payment.domain.payment.enums.PaymentMethod;
import br.com.accenture.payment.domain.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        String externalTransactionId,
        String failureReason,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt
) {
}