package br.com.accenture.auth.domain.model;

import br.com.accenture.auth.domain.enums.Role;
import br.com.accenture.auth.domain.service.PasswordEncoder;
import br.com.accenture.auth.domain.vo.Email;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserCredentialTest {

    private final PasswordEncoder encoder = new PasswordEncoder() {
        @Override public String encode(String password) { return "hashed:" + password; }
        @Override public boolean matches(String password, String encodedPassword) {
            return encodedPassword.equals("hashed:" + password);
        }
    };

    @Test
    void registerNew_shouldCreateActiveCredentialWithGeneratedId() {
        UUID customerId = UUID.randomUUID();

        UserCredential credential = UserCredential.registerNew(
                customerId,
                "user@example.com",
                "secret-pass",
                Set.of(Role.CUSTOMER),
                encoder
        );

        assertThat(credential.getId()).isNotNull();
        assertThat(credential.getCustomerId()).isEqualTo(customerId);
        assertThat(credential.getEmail()).isEqualTo(new Email("user@example.com"));
        assertThat(credential.getPassword().value()).isEqualTo("hashed:secret-pass");
        assertThat(credential.getRoles()).containsExactly(Role.CUSTOMER);
        assertThat(credential.isActive()).isTrue();
        assertThat(credential.getCreatedAt()).isNotNull();
        assertThat(credential.getUpdatedAt()).isNotNull();
    }

    @Test
    void registerNew_shouldThrowWhenCustomerIdIsNull() {
        assertThatThrownBy(() -> UserCredential.registerNew(
                null, "user@example.com", "secret-pass", Set.of(Role.CUSTOMER), encoder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer ID");
    }

    @Test
    void registerNew_shouldThrowWhenRolesIsNull() {
        UUID customerId = UUID.randomUUID();
        assertThatThrownBy(() -> UserCredential.registerNew(
                customerId, "user@example.com", "secret-pass", null, encoder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("role");
    }

    @Test
    void registerNew_shouldThrowWhenRolesIsEmpty() {
        UUID customerId = UUID.randomUUID();
        assertThatThrownBy(() -> UserCredential.registerNew(
                customerId, "user@example.com", "secret-pass", Set.of(), encoder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("role");
    }

    @Test
    void restore_shouldRecreateExistingCredentialAsIs() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2025-01-02T10:00:00Z");

        UserCredential credential = UserCredential.restore(
                id, customerId, "user@example.com", "$2a$10$hashed",
                Set.of(Role.ADMIN, Role.CUSTOMER), false, createdAt, updatedAt
        );

        assertThat(credential.getId()).isEqualTo(id);
        assertThat(credential.getCustomerId()).isEqualTo(customerId);
        assertThat(credential.getPassword().value()).isEqualTo("$2a$10$hashed");
        assertThat(credential.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.CUSTOMER);
        assertThat(credential.isActive()).isFalse();
        assertThat(credential.getCreatedAt()).isEqualTo(createdAt);
        assertThat(credential.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void changePassword_shouldReplacePasswordAndUpdateTimestamp() throws InterruptedException {
        UserCredential credential = UserCredential.registerNew(
                UUID.randomUUID(), "user@example.com", "old-pass1",
                Set.of(Role.CUSTOMER), encoder
        );
        Instant before = credential.getUpdatedAt();
        Thread.sleep(5);

        credential.changePassword("new-pass2", encoder);

        assertThat(credential.getPassword().value()).isEqualTo("hashed:new-pass2");
        assertThat(credential.getUpdatedAt()).isAfter(before);
    }

    @Test
    void changeEmail_shouldReplaceEmailAndUpdateTimestamp() throws InterruptedException {
        UserCredential credential = UserCredential.registerNew(
                UUID.randomUUID(), "user@example.com", "old-pass1",
                Set.of(Role.CUSTOMER), encoder
        );
        Instant before = credential.getUpdatedAt();
        Thread.sleep(5);

        credential.changeEmail("new@example.com");

        assertThat(credential.getEmail()).isEqualTo(new Email("new@example.com"));
        assertThat(credential.getUpdatedAt()).isAfter(before);
    }

    @Test
    void deactivate_shouldFlipActiveToFalse() {
        UserCredential credential = UserCredential.registerNew(
                UUID.randomUUID(), "user@example.com", "secret-pass",
                Set.of(Role.CUSTOMER), encoder
        );

        credential.deactivate();

        assertThat(credential.isActive()).isFalse();
    }

    @Test
    void deactivate_shouldThrowWhenAlreadyInactive() {
        UserCredential credential = UserCredential.restore(
                UUID.randomUUID(), UUID.randomUUID(), "user@example.com",
                "$2a$10$hashed", Set.of(Role.CUSTOMER), false,
                Instant.now(), Instant.now()
        );

        assertThatThrownBy(credential::deactivate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already inactive");
    }

    @Test
    void getRoles_shouldReturnUnmodifiableSet() {
        UserCredential credential = UserCredential.registerNew(
                UUID.randomUUID(), "user@example.com", "secret-pass",
                new HashSet<>(Set.of(Role.CUSTOMER)), encoder
        );

        assertThatThrownBy(() -> credential.getRoles().add(Role.ADMIN))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
