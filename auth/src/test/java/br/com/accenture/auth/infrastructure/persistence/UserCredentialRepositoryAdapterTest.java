package br.com.accenture.auth.infrastructure.persistence;

import br.com.accenture.auth.domain.enums.Role;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.vo.Email;
import br.com.accenture.auth.infrastructure.persistence.entity.UserCredentialEntity;
import br.com.accenture.auth.infrastructure.persistence.mapper.UserCredentialMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCredentialRepositoryAdapterTest {

    @Mock
    private SpringDataUserCredentialRepository jpaRepository;
    @Mock
    private UserCredentialMapper mapper;

    @InjectMocks
    private UserCredentialRepositoryAdapter adapter;

    private UserCredential domain;
    private UserCredentialEntity entity;

    @BeforeEach
    void setUp() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        domain = UserCredential.restore(id, customerId, "user@example.com", "$2a$10$h",
                Set.of(Role.CUSTOMER), true, Instant.now(), Instant.now());
        entity = new UserCredentialEntity(id, customerId, "user@example.com", "$2a$10$h",
                Set.of(Role.CUSTOMER), true, Instant.now(), Instant.now());
    }

    @Test
    void save_shouldMapDomainAndDelegateToJpa() {
        when(mapper.toEntity(domain)).thenReturn(entity);

        adapter.save(domain);

        verify(jpaRepository).save(entity);
    }

    @Test
    void findByEmail_shouldReturnDomainWhenFound() {
        when(jpaRepository.findByEmail("user@example.com")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<UserCredential> result = adapter.findByEmail(new Email("user@example.com"));

        assertThat(result).contains(domain);
    }

    @Test
    void findByEmail_shouldReturnEmptyWhenNotFound() {
        when(jpaRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        Optional<UserCredential> result = adapter.findByEmail(new Email("missing@example.com"));

        assertThat(result).isEmpty();
    }

    @Test
    void findByCustomerId_shouldReturnDomainWhenFound() {
        UUID customerId = UUID.randomUUID();
        when(jpaRepository.findByCustomerId(customerId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<UserCredential> result = adapter.findByCustomerId(customerId);

        assertThat(result).contains(domain);
    }

    @Test
    void findByCustomerId_shouldReturnEmptyWhenNotFound() {
        UUID customerId = UUID.randomUUID();
        when(jpaRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThat(adapter.findByCustomerId(customerId)).isEmpty();
    }

    @Test
    void existsByEmail_shouldDelegateToJpa() {
        when(jpaRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThat(adapter.existsByEmail(new Email("user@example.com"))).isTrue();
    }
}
