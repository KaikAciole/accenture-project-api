package br.com.accenture.auth.domain.vo;

import br.com.accenture.auth.domain.service.PasswordEncoder;

public record Password(String value) {

    public Password {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be null or empty");
        }
    }

    public static Password create(String rawPassword, PasswordEncoder encoder) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Raw password must be at least 8 characters long");
        }
        return new Password(encoder.encode(rawPassword));
    }

    public static Password restore(String hashedPassword) {
        return new Password(hashedPassword);
    }
}