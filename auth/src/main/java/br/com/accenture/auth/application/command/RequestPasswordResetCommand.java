package br.com.accenture.auth.application.command;

public record RequestPasswordResetCommand(
        String email
) {}
