package br.com.accenture.auth.application.command;

import java.util.UUID;

public record ChangeEmailCommand(
        UUID customerId,
        String newEmail
) {}
