package br.com.accenture.auth.application.usecase;

import br.com.accenture.auth.application.command.RegisterCommand;
import br.com.accenture.auth.application.exception.UserAlreadyExistsException;
import br.com.accenture.auth.application.publisher.AuthEventPublisher;
import br.com.accenture.auth.domain.enums.Role;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.repository.UserCredentialRepository;
import br.com.accenture.auth.domain.service.PasswordEncoder;
import br.com.accenture.auth.domain.vo.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserCredentialRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthEventPublisher eventPublisher;

    @InjectMocks
    private RegisterUserUseCase useCase;

    @Test
    void execute_shouldSaveUserAndPublishEventOnHappyPath() {
        UUID customerId = UUID.randomUUID();
        when(repository.existsByEmail(new Email("user@example.com"))).thenReturn(false);
        when(passwordEncoder.encode("rawPass123")).thenReturn("$2a$10$hashed");

        useCase.execute(new RegisterCommand(customerId, "user@example.com", "rawPass123", Set.of("customer")));

        ArgumentCaptor<UserCredential> userCaptor = ArgumentCaptor.forClass(UserCredential.class);
        verify(repository).save(userCaptor.capture());
        verify(eventPublisher).publishUserRegisteredEvent(userCaptor.getValue());
    }

    @Test
    void execute_shouldUppercaseRoleNames() {
        UUID customerId = UUID.randomUUID();
        when(repository.existsByEmail(new Email("admin@example.com"))).thenReturn(false);
        when(passwordEncoder.encode("rawPass123")).thenReturn("$2a$10$hashed");

        useCase.execute(new RegisterCommand(customerId, "admin@example.com", "rawPass123", Set.of("admin", "customer")));

        ArgumentCaptor<UserCredential> userCaptor = ArgumentCaptor.forClass(UserCredential.class);
        verify(repository).save(userCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(userCaptor.getValue().getRoles())
                .containsExactlyInAnyOrder(Role.ADMIN, Role.CUSTOMER);
    }

    @Test
    void execute_shouldThrowWhenEmailAlreadyExists() {
        UUID customerId = UUID.randomUUID();
        when(repository.existsByEmail(new Email("dup@example.com"))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(
                new RegisterCommand(customerId, "dup@example.com", "rawPass123", Set.of("customer"))))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishUserRegisteredEvent(any());
    }
}
