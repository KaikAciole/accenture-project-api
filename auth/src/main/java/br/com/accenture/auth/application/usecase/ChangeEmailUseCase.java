package br.com.accenture.auth.application.usecase;

import br.com.accenture.auth.application.command.ChangeEmailCommand;
import br.com.accenture.auth.application.exception.CredentialNotFoundException;
import br.com.accenture.auth.application.exception.UserAlreadyExistsException;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.repository.UserCredentialRepository;
import br.com.accenture.auth.domain.vo.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeEmailUseCase {

    private final UserCredentialRepository repository;

    @Transactional
    public void execute(ChangeEmailCommand command) {
        UserCredential user = repository.findByCustomerId(command.customerId())
                .orElseThrow(() -> new CredentialNotFoundException(
                        "Credential not found for customer id: " + command.customerId()));

        Email newEmail = new Email(command.newEmail());

        if (user.getEmail().equals(newEmail)) {
            return;
        }

        if (repository.existsByEmail(newEmail)) {
            throw new UserAlreadyExistsException("User already exists with this email.");
        }

        user.changeEmail(command.newEmail());

        repository.save(user);
    }
}
