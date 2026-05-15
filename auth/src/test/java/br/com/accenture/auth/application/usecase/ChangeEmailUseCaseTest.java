package br.com.accenture.auth.application.usecase;

import br.com.accenture.auth.application.command.ChangeEmailCommand;
import br.com.accenture.auth.application.exception.CredentialNotFoundException;
import br.com.accenture.auth.application.exception.UserAlreadyExistsException;
import br.com.accenture.auth.domain.enums.Role;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.repository.UserCredentialRepository;
import br.com.accenture.auth.domain.vo.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeEmailUseCaseTest {

    @Mock
    private UserCredentialRepository repository;

    @InjectMocks
    private ChangeEmailUseCase useCase;

    private UUID customerId;
    private UserCredential existing;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        existing = UserCredential.restore(
                UUID.randomUUID(),
                customerId,
                "old@example.com",
                "$2a$10$hashedPassword",
                Set.of(Role.CUSTOMER),
                true,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    void execute_shouldChangeEmailWhenAllValidationsPass() {
        when(repository.findByCustomerId(customerId)).thenReturn(java.util.Optional.of(existing));
        when(repository.existsByEmail(new Email("new@example.com"))).thenReturn(false);

        useCase.execute(new ChangeEmailCommand(customerId, "new@example.com"));

        ArgumentCaptor<UserCredential> captor = ArgumentCaptor.forClass(UserCredential.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEmail().value()).isEqualTo("new@example.com");
    }

    @Test
    void execute_shouldDoNothingWhenEmailIsTheSame() {
        when(repository.findByCustomerId(customerId)).thenReturn(java.util.Optional.of(existing));

        useCase.execute(new ChangeEmailCommand(customerId, "old@example.com"));

        verify(repository, never()).existsByEmail(any());
        verify(repository, never()).save(any());
    }

    @Test
    void execute_shouldThrowWhenCredentialNotFoundForCustomer() {
        when(repository.findByCustomerId(customerId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ChangeEmailCommand(customerId, "new@example.com")))
                .isInstanceOf(CredentialNotFoundException.class)
                .hasMessageContaining(customerId.toString());

        verify(repository, never()).save(any());
    }

    @Test
    void execute_shouldThrowWhenNewEmailAlreadyExists() {
        when(repository.findByCustomerId(customerId)).thenReturn(java.util.Optional.of(existing));
        when(repository.existsByEmail(new Email("taken@example.com"))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new ChangeEmailCommand(customerId, "taken@example.com")))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(repository, never()).save(any());
    }
}
