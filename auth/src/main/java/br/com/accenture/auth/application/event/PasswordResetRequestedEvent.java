package br.com.accenture.auth.application.event;

import java.util.UUID;

public record PasswordResetRequestedEvent(
        UUID customerId,
        String email,
        String token
) {}
