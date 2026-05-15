package br.com.accenture.auth.application.usecase;

import br.com.accenture.auth.application.command.RegisterCommand;
import br.com.accenture.auth.application.exception.UserAlreadyExistsException;
import br.com.accenture.auth.application.publisher.AuthEventPublisher;
import br.com.accenture.auth.domain.enums.Role;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.repository.UserCredentialRepository;
import br.com.accenture.auth.domain.service.PasswordEncoder;
import br.com.accenture.auth.domain.vo.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthEventPublisher eventPublisher;

    @Transactional
    public void execute(RegisterCommand command) {
        Email email = new Email(command.email());

        if (repository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User already exists with this email.");
        }

        Set<Role> roles = command.roles().stream()
                .map(String::toUpperCase)
                .map(Role::valueOf)
                .collect(Collectors.toSet());

        UserCredential user = UserCredential.registerNew(
                command.customerId(),
                command.email(),
                command.password(),
                roles,
                passwordEncoder
        );

        repository.save(user);

        // A mágica do Outbox acontece aqui dentro, de forma invisível para o Use Case
        eventPublisher.publishUserRegisteredEvent(user);
    }
}