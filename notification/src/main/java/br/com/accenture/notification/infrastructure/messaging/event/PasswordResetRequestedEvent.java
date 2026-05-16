package br.com.accenture.notification.infrastructure.messaging.event;

import java.util.UUID;

public record PasswordResetRequestedEvent(
        UUID customerId,
        String email,
        String token
) {}
