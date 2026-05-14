package br.com.accenture.payment.infrastructure.messaging.event;

import java.util.UUID;

public record OrderCanceledEvent(
        UUID orderId,
        String customerId,
        String reason
) {
}