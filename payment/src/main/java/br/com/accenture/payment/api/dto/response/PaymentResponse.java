package br.com.accenture.payment.api.dto.response;

import br.com.accenture.payment.domain.enums.PaymentMethod;
import br.com.accenture.payment.domain.enums.PaymentStatus;

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