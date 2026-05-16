package br.com.accenture.auth.application.usecase;

import br.com.accenture.auth.application.command.ResetPasswordCommand;
import br.com.accenture.auth.application.exception.InvalidPasswordResetTokenException;
import br.com.accenture.auth.domain.enums.Role;
import br.com.accenture.auth.domain.model.PasswordResetToken;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.repository.PasswordResetTokenRepository;
import br.com.accenture.auth.domain.repository.UserCredentialRepository;
import br.com.accenture.auth.domain.service.PasswordEncoder;
import br.com.accenture.auth.domain.vo.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetPasswordUseCaseTest {

    @Mock
    private UserCredentialRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ResetPasswordUseCase useCase;

    private UUID customerId;
    private UserCredential user;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        user = UserCredential.restore(
                UUID.randomUUID(),
                customerId,
                "user@example.com",
                "$2a$10$oldHashedPassword",
                Set.of(Role.CUSTOMER),
                true,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    void execute_shouldChangePasswordAndMarkTokenAsUsedOnHappyPath() {
        PasswordResetToken token = PasswordResetToken.issue(
                customerId,
                "hashed:plain-token",
                Instant.now().plus(15, ChronoUnit.MINUTES)
        );
        when(userRepository.findByEmail(new Email("user@example.com"))).thenReturn(Optional.of(user));
        when(tokenRepository.findUsableByCustomerId(customerId)).thenReturn(List.of(token));
        when(passwordEncoder.matches("plain-token", "hashed:plain-token")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$newHashed");

        useCase.execute(new ResetPasswordCommand("user@example.com", "plain-token", "newPass123"));

        verify(tokenRepository).save(token);
        verify(userRepository).save(user);
    }

    @Test
    void execute_shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmail(new Email("unknown@example.com"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ResetPasswordCommand("unknown@example.com", "token", "newPass123")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowWhenTokenDoesNotMatchAnyHash() {
        PasswordResetToken token = PasswordResetToken.issue(
                customerId,
                "hashed:other-token",
                Instant.now().plus(15, ChronoUnit.MINUTES)
        );
        when(userRepository.findByEmail(new Email("user@example.com"))).thenReturn(Optional.of(user));
        when(tokenRepository.findUsableByCustomerId(customerId)).thenReturn(List.of(token));
        when(passwordEncoder.matches("plain-token", "hashed:other-token")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ResetPasswordCommand("user@example.com", "plain-token", "newPass123")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        verify(tokenRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowWhenNoUsableTokensExist() {
        when(userRepository.findByEmail(new Email("user@example.com"))).thenReturn(Optional.of(user));
        when(tokenRepository.findUsableByCustomerId(customerId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new ResetPasswordCommand("user@example.com", "plain-token", "newPass123")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        verify(userRepository, never()).save(any());
    }
}
