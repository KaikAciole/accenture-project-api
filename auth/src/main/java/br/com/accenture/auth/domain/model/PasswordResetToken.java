package br.com.accenture.auth.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class PasswordResetToken {

    private final UUID id;
    private final UUID customerId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant usedAt;

    private PasswordResetToken(UUID id,
                               UUID customerId,
                               String tokenHash,
                               Instant expiresAt,
                               Instant createdAt,
                               Instant usedAt) {
        this.id = id;
        this.customerId = customerId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.usedAt = usedAt;
    }

    public static PasswordResetToken issue(UUID customerId, String tokenHash, Instant expiresAt) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId is required");
        }
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("tokenHash is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }
        return new PasswordResetToken(UUID.randomUUID(), customerId, tokenHash, expiresAt, Instant.now(), null);
    }

    public static PasswordResetToken restore(UUID id,
                                             UUID customerId,
                                             String tokenHash,
                                             Instant expiresAt,
                                             Instant createdAt,
                                             Instant usedAt) {
        return new PasswordResetToken(id, customerId, tokenHash, expiresAt, createdAt, usedAt);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isUsable() {
        return !isUsed() && !isExpired();
    }

    public void markAsUsed() {
        if (isUsed()) {
            throw new IllegalStateException("Token has already been used");
        }
        if (isExpired()) {
            throw new IllegalStateException("Token has expired");
        }
        this.usedAt = Instant.now();
    }
}
