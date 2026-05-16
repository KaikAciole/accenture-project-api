package br.com.accenture.auth.application.usecase;

import br.com.accenture.auth.application.command.RequestPasswordResetCommand;
import br.com.accenture.auth.application.publisher.AuthEventPublisher;
import br.com.accenture.auth.domain.model.PasswordResetToken;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.repository.PasswordResetTokenRepository;
import br.com.accenture.auth.domain.repository.UserCredentialRepository;
import br.com.accenture.auth.domain.service.PasswordEncoder;
import br.com.accenture.auth.domain.vo.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestPasswordResetUseCase {

    private final UserCredentialRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthEventPublisher eventPublisher;

    @Value("${password-reset.token-ttl-minutes:30}")
    private long tokenTtlMinutes;

    @Transactional
    public void execute(RequestPasswordResetCommand command) {
        Optional<UserCredential> userOpt = userRepository.findByEmail(new Email(command.email()));

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for unknown email; returning silently for security.");
            return;
        }

        UserCredential user = userOpt.get();

        String plainToken = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(plainToken);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(tokenTtlMinutes));

        PasswordResetToken token = PasswordResetToken.issue(user.getCustomerId(), tokenHash, expiresAt);
        tokenRepository.save(token);

        eventPublisher.publishPasswordResetRequestedEvent(user.getEmail().value(), plainToken);
    }
}
