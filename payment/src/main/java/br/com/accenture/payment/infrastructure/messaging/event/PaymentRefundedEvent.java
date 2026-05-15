package br.com.accenture.payment.infrastructure.messaging.event;

import br.com.accenture.payment.domain.payment.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRefundedEvent(
        UUID eventId,
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        PaymentMethod method,
        String reason,
        Instant occurredAt
) {
}
