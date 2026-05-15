package br.com.accenture.auth.domain.model;

import br.com.accenture.auth.domain.enums.Role;
import br.com.accenture.auth.domain.service.PasswordEncoder;
import br.com.accenture.auth.domain.vo.Email;
import br.com.accenture.auth.domain.vo.Password;
import lombok.Getter;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public class UserCredential {

    private final UUID id;
    private final UUID customerId;
    private final Email email;
    private Password password;
    private final Set<Role> roles;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private UserCredential(UUID id, UUID customerId, Email email, Password password,
                           Set<Role> roles, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.email = email;
        this.password = password;
        this.roles = new HashSet<>(roles);
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserCredential registerNew(UUID customerId, String rawEmail, String password,
                                             Set<Role> roles, PasswordEncoder encoder) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }

        return new UserCredential(
                UUID.randomUUID(),
                customerId,
                new Email(rawEmail),
                Password.create(password, encoder),
                roles,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    public static UserCredential restore(UUID id, UUID customerId, String emailValue, String hashedPassword,
                                         Set<Role> roles, boolean active, Instant createdAt, Instant updatedAt) {
        return new UserCredential(
                id,
                customerId,
                new Email(emailValue),
                Password.restore(hashedPassword),
                roles,
                active,
                createdAt,
                updatedAt
        );
    }

    public void changePassword(String newPassword, PasswordEncoder encoder) {
        this.password = Password.create(newPassword, encoder);
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        if (!this.active) {
            throw new IllegalStateException("User credential is already inactive");
        }
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }
}