package br.com.accenture.order.infrastructure.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentApprovedEvent(
        UUID eventId,
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String method,
        Instant paidAt,
        Instant occurredAt
) {}