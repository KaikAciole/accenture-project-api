package br.com.accenture.auth.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetTokenTest {

    @Test
    void issue_shouldCreateUsableToken() {
        UUID customerId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        PasswordResetToken token = PasswordResetToken.issue(customerId, "hash:abc", expiresAt);

        assertThat(token.getId()).isNotNull();
        assertThat(token.getCustomerId()).isEqualTo(customerId);
        assertThat(token.getTokenHash()).isEqualTo("hash:abc");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.getCreatedAt()).isNotNull();
        assertThat(token.getUsedAt()).isNull();
        assertThat(token.isUsed()).isFalse();
        assertThat(token.isExpired()).isFalse();
        assertThat(token.isUsable()).isTrue();
    }

    @Test
    void issue_shouldThrowWhenCustomerIdIsNull() {
        assertThatThrownBy(() -> PasswordResetToken.issue(null, "hash", Instant.now().plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customerId");
    }

    @Test
    void issue_shouldThrowWhenTokenHashIsNull() {
        assertThatThrownBy(() -> PasswordResetToken.issue(UUID.randomUUID(), null, Instant.now().plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokenHash");
    }

    @Test
    void issue_shouldThrowWhenTokenHashIsBlank() {
        assertThatThrownBy(() -> PasswordResetToken.issue(UUID.randomUUID(), "   ", Instant.now().plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokenHash");
    }

    @Test
    void issue_shouldThrowWhenExpiresAtIsNull() {
        assertThatThrownBy(() -> PasswordResetToken.issue(UUID.randomUUID(), "hash", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresAt");
    }

    @Test
    void restore_shouldRebuildAnyTokenAsIs() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2025-01-01T10:15:00Z");
        Instant createdAt = Instant.parse("2025-01-01T10:00:00Z");
        Instant usedAt = Instant.parse("2025-01-01T10:05:00Z");

        PasswordResetToken token = PasswordResetToken.restore(id, customerId, "hash", expiresAt, createdAt, usedAt);

        assertThat(token.getId()).isEqualTo(id);
        assertThat(token.getCustomerId()).isEqualTo(customerId);
        assertThat(token.getTokenHash()).isEqualTo("hash");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.getCreatedAt()).isEqualTo(createdAt);
        assertThat(token.getUsedAt()).isEqualTo(usedAt);
        assertThat(token.isUsed()).isTrue();
        assertThat(token.isUsable()).isFalse();
    }

    @Test
    void isExpired_shouldReturnTrueWhenExpiresAtIsInThePast() {
        PasswordResetToken token = PasswordResetToken.restore(
                UUID.randomUUID(), UUID.randomUUID(), "hash",
                Instant.now().minusSeconds(60), Instant.now().minusSeconds(120), null
        );

        assertThat(token.isExpired()).isTrue();
        assertThat(token.isUsable()).isFalse();
    }

    @Test
    void markAsUsed_shouldSetUsedAtOnUsableToken() {
        PasswordResetToken token = PasswordResetToken.issue(
                UUID.randomUUID(), "hash", Instant.now().plusSeconds(60)
        );

        token.markAsUsed();

        assertThat(token.getUsedAt()).isNotNull();
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void markAsUsed_shouldThrowWhenAlreadyUsed() {
        PasswordResetToken token = PasswordResetToken.restore(
                UUID.randomUUID(), UUID.randomUUID(), "hash",
                Instant.now().plusSeconds(60), Instant.now(), Instant.now()
        );

        assertThatThrownBy(token::markAsUsed)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void markAsUsed_shouldThrowWhenExpired() {
        PasswordResetToken token = PasswordResetToken.restore(
                UUID.randomUUID(), UUID.randomUUID(), "hash",
                Instant.now().minusSeconds(60), Instant.now().minusSeconds(120), null
        );

        assertThatThrownBy(token::markAsUsed)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
    }
}
