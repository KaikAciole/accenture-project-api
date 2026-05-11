package br.com.accenture.auth.application.usecase;

import br.com.accenture.auth.application.command.RegisterCommand;
import br.com.accenture.auth.application.exception.UserAlreadyExistsException;
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

    @Transactional
    public void execute(RegisterCommand command) {
        Email emailVo = new Email(command.email());

        if (repository.existsByEmail(emailVo)) {
            throw new UserAlreadyExistsException("Email is already registered.");
        }

        Set<Role> domainRoles = command.roles().stream()
                .map(String::toUpperCase)
                .map(Role::valueOf)
                .collect(Collectors.toSet());

        UserCredential newCredential = UserCredential.registerNew(
                command.customerId(),
                command.email(),
                command.rawPassword(),
                domainRoles,
                passwordEncoder
        );

        repository.save(newCredential);
    }
}