package br.com.accenture.auth.infrastructure.persistence.mapper;

import br.com.accenture.auth.domain.enums.Role;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.infrastructure.persistence.entity.UserCredentialEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserCredentialMapperTest {

    private final UserCredentialMapper mapper = new UserCredentialMapper();

    @Test
    void toEntity_shouldMapAllFields() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2025-01-02T10:00:00Z");
        UserCredential domain = UserCredential.restore(
                id, customerId, "user@example.com", "$2a$10$hashed",
                Set.of(Role.CUSTOMER), true, createdAt, updatedAt
        );

        UserCredentialEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getCustomerId()).isEqualTo(customerId);
        assertThat(entity.getEmail()).isEqualTo("user@example.com");
        assertThat(entity.getPassword()).isEqualTo("$2a$10$hashed");
        assertThat(entity.getRoles()).containsExactly(Role.CUSTOMER);
        assertThat(entity.isActive()).isTrue();
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toEntity_shouldReturnNullWhenDomainIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDomain_shouldMapAllFields() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UserCredentialEntity entity = new UserCredentialEntity(
                id, customerId, "user@example.com", "$2a$10$hashed",
                Set.of(Role.ADMIN), false,
                Instant.parse("2025-01-01T10:00:00Z"),
                Instant.parse("2025-01-02T10:00:00Z")
        );

        UserCredential domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getCustomerId()).isEqualTo(customerId);
        assertThat(domain.getEmail().value()).isEqualTo("user@example.com");
        assertThat(domain.getPassword().value()).isEqualTo("$2a$10$hashed");
        assertThat(domain.getRoles()).containsExactly(Role.ADMIN);
        assertThat(domain.isActive()).isFalse();
    }

    @Test
    void toDomain_shouldReturnNullWhenEntityIsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }
}
