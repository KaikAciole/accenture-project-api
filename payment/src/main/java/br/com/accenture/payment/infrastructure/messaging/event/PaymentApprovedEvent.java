package br.com.accenture.payment.infrastructure.messaging.event;

import br.com.accenture.payment.domain.payment.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentApprovedEvent(
        UUID eventId,
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        PaymentMethod method,
        Instant paidAt,
        Instant occurredAt
) {
}