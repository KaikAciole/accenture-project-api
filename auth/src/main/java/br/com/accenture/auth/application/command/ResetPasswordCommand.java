package br.com.accenture.auth.application.command;

public record ResetPasswordCommand(
        String email,
        String token,
        String newPassword
) {}
