package br.com.accenture.payment.infrastructure.messaging.event;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID customerId,
        String email
) {
}