package br.com.accenture.auth.application.usecase;

import br.com.accenture.auth.application.command.ResetPasswordCommand;
import br.com.accenture.auth.application.exception.InvalidPasswordResetTokenException;
import br.com.accenture.auth.domain.model.PasswordResetToken;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.repository.PasswordResetTokenRepository;
import br.com.accenture.auth.domain.repository.UserCredentialRepository;
import br.com.accenture.auth.domain.service.PasswordEncoder;
import br.com.accenture.auth.domain.vo.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private final UserCredentialRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(ResetPasswordCommand command) {
        UserCredential user = userRepository.findByEmail(new Email(command.email()))
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid or expired token."));

        List<PasswordResetToken> usableTokens = tokenRepository.findUsableByCustomerId(user.getCustomerId());

        PasswordResetToken matchingToken = usableTokens.stream()
                .filter(t -> passwordEncoder.matches(command.token(), t.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid or expired token."));

        matchingToken.markAsUsed();
        tokenRepository.save(matchingToken);

        user.changePassword(command.newPassword(), passwordEncoder);
        userRepository.save(user);
    }
}
