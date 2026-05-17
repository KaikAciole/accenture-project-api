package br.com.accenture.auth.infrastructure.persistence;

import br.com.accenture.auth.domain.model.PasswordResetToken;
import br.com.accenture.auth.infrastructure.persistence.entity.PasswordResetTokenEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenRepositoryAdapterTest {

    @Mock
    private SpringDataPasswordResetTokenRepository jpaRepository;

    @InjectMocks
    private PasswordResetTokenRepositoryAdapter adapter;

    @Test
    void save_shouldPersistEntityWithAllDomainFields() {
        UUID customerId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(900);
        PasswordResetToken token = PasswordResetToken.issue(customerId, "hash:abc", expiresAt);

        adapter.save(token);

        ArgumentCaptor<PasswordResetTokenEntity> captor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(jpaRepository).save(captor.capture());
        PasswordResetTokenEntity saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(token.getId());
        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getTokenHash()).isEqualTo("hash:abc");
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(saved.getCreatedAt()).isEqualTo(token.getCreatedAt());
        assertThat(saved.getUsedAt()).isNull();
    }

    @Test
    void findUsableByCustomerId_shouldMapEntitiesToDomain() {
        UUID customerId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(900);
        Instant createdAt = Instant.now().minusSeconds(60);
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity(
                entityId, customerId, "hash:abc", expiresAt, createdAt, null
        );
        when(jpaRepository.findAllByCustomerIdAndUsedAtIsNullAndExpiresAtAfter(eq(customerId), any(Instant.class)))
                .thenReturn(List.of(entity));

        List<PasswordResetToken> result = adapter.findUsableByCustomerId(customerId);

        assertThat(result).hasSize(1);
        PasswordResetToken token = result.get(0);
        assertThat(token.getId()).isEqualTo(entityId);
        assertThat(token.getCustomerId()).isEqualTo(customerId);
        assertThat(token.getTokenHash()).isEqualTo("hash:abc");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.getCreatedAt()).isEqualTo(createdAt);
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    void findUsableByCustomerId_shouldReturnEmptyListWhenNoResults() {
        UUID customerId = UUID.randomUUID();
        when(jpaRepository.findAllByCustomerIdAndUsedAtIsNullAndExpiresAtAfter(eq(customerId), any(Instant.class)))
                .thenReturn(List.of());

        assertThat(adapter.findUsableByCustomerId(customerId)).isEmpty();
    }
}
