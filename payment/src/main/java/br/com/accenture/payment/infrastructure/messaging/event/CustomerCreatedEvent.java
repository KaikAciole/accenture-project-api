package br.com.accenture.payment.infrastructure.messaging.event;

import java.util.UUID;

public record CustomerCreatedEvent(
        UUID customerId,
        String name,
        String email
) {
}