package br.com.accenture.auth.application.command;

public record LoginCommand(
        String email,
        String rawPassword
) {}