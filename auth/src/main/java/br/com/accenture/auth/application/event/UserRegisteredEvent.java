package br.com.accenture.auth.application.event;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID customerId,
        String email
) {}