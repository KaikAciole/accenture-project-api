package br.com.accenture.auth.application.usecase;

import br.com.accenture.auth.application.command.LoginCommand;
import br.com.accenture.auth.application.exception.InvalidCredentialsException;
import br.com.accenture.auth.domain.enums.Role;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.repository.UserCredentialRepository;
import br.com.accenture.auth.domain.service.PasswordEncoder;
import br.com.accenture.auth.domain.service.TokenProvider;
import br.com.accenture.auth.domain.vo.Email;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

    @Mock
    private UserCredentialRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private AuthenticateUserUseCase useCase;

    private UserCredential activeUser;
    private UserCredential inactiveUser;

    @BeforeEach
    void setUp() {
        activeUser = UserCredential.restore(
                UUID.randomUUID(), UUID.randomUUID(), "user@example.com",
                "$2a$10$hashed", Set.of(Role.CUSTOMER), true,
                Instant.now(), Instant.now()
        );
        inactiveUser = UserCredential.restore(
                UUID.randomUUID(), UUID.randomUUID(), "user@example.com",
                "$2a$10$hashed", Set.of(Role.CUSTOMER), false,
                Instant.now(), Instant.now()
        );
    }

    @Test
    void execute_shouldReturnTokenOnHappyPath() {
        when(repository.findByEmail(new Email("user@example.com"))).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("plainPass", "$2a$10$hashed")).thenReturn(true);
        when(tokenProvider.generateAccessToken(activeUser)).thenReturn("jwt-token");

        String token = useCase.execute(new LoginCommand("user@example.com", "plainPass"));

        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    void execute_shouldThrowWhenUserNotFound() {
        when(repository.findByEmail(new Email("missing@example.com"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new LoginCommand("missing@example.com", "plainPass")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void execute_shouldThrowWhenPasswordDoesNotMatch() {
        when(repository.findByEmail(new Email("user@example.com"))).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrongPass", "$2a$10$hashed")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new LoginCommand("user@example.com", "wrongPass")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void execute_shouldThrowWhenUserIsInactive() {
        when(repository.findByEmail(new Email("user@example.com"))).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("plainPass", "$2a$10$hashed")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new LoginCommand("user@example.com", "plainPass")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("disabled");
    }
}
