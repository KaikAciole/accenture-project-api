package br.com.accenture.notification.infrastructure.messaging.event;

public record PasswordResetRequestedEvent(
        String email,
        String token
) {}
