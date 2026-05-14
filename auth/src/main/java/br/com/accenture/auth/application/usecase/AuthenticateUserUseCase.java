package br.com.accenture.auth.application.usecase;

import br.com.accenture.auth.application.command.LoginCommand;
import br.com.accenture.auth.application.exception.InvalidCredentialsException;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.repository.UserCredentialRepository;
import br.com.accenture.auth.domain.service.PasswordEncoder;
import br.com.accenture.auth.domain.service.TokenProvider;
import br.com.accenture.auth.domain.vo.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticateUserUseCase {

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Transactional(readOnly = true)
    public String execute(LoginCommand command) {
        Email emailVo = new Email(command.email());

        UserCredential user = repository.findByEmail(emailVo)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(command.password(), user.getPassword().value())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        if (!user.isActive()) {
            throw new InvalidCredentialsException("User account is disabled.");
        }

        return tokenProvider.generateAccessToken(user);
    }
}