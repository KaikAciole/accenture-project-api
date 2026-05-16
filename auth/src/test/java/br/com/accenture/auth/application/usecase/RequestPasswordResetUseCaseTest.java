package br.com.accenture.auth.application.usecase;

import br.com.accenture.auth.application.command.RequestPasswordResetCommand;
import br.com.accenture.auth.application.publisher.AuthEventPublisher;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestPasswordResetUseCaseTest {

    @Mock
    private UserCredentialRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthEventPublisher eventPublisher;

    @InjectMocks
    private RequestPasswordResetUseCase useCase;

    private UUID customerId;
    private UserCredential user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "tokenTtlMinutes", 30L);
        customerId = UUID.randomUUID();
        user = UserCredential.restore(
                UUID.randomUUID(),
                customerId,
                "user@example.com",
                "$2a$10$hashedPassword",
                Set.of(Role.CUSTOMER),
                true,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    void execute_shouldGenerateTokenAndPublishEventWhenUserExists() {
        when(userRepository.findByEmail(new Email("user@example.com"))).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(any())).thenAnswer(inv -> "hashed:" + inv.getArgument(0));

        useCase.execute(new RequestPasswordResetCommand("user@example.com"));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getCustomerId()).isEqualTo(customerId);
        assertThat(tokenCaptor.getValue().getTokenHash()).startsWith("hashed:");
        assertThat(tokenCaptor.getValue().getExpiresAt()).isAfter(Instant.now());

        verify(eventPublisher).publishPasswordResetRequestedEvent(eq(customerId), eq("user@example.com"), any());
    }

    @Test
    void execute_shouldSilentlyReturnWhenUserDoesNotExist() {
        when(userRepository.findByEmail(new Email("unknown@example.com"))).thenReturn(Optional.empty());

        useCase.execute(new RequestPasswordResetCommand("unknown@example.com"));

        verify(tokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishPasswordResetRequestedEvent(any(), any(), any());
    }
}
