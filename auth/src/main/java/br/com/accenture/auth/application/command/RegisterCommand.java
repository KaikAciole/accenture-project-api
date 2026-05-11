package br.com.accenture.auth.application.command;

import java.util.Set;
import java.util.UUID;

public record RegisterCommand(
        UUID customerId,
        String email,
        String rawPassword,
        Set<String> roles
) {}