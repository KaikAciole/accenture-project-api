package br.com.accenture.auth.application.event;

public record PasswordResetRequestedEvent(
        String email,
        String token
) {}
