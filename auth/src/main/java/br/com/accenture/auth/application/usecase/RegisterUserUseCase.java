package br.com.accenture.auth.application.usecase;

import br.com.accenture.auth.application.command.RegisterCommand;
import br.com.accenture.auth.application.event.UserRegisteredEvent;
import br.com.accenture.auth.application.exception.UserAlreadyExistsException;
import br.com.accenture.auth.domain.enums.Role;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.repository.UserCredentialRepository;
import br.com.accenture.auth.domain.service.PasswordEncoder;
import br.com.accenture.auth.domain.vo.Email;
import br.com.accenture.auth.infrastructure.persistence.entity.OutboxEventJpaEntity;
import br.com.accenture.auth.infrastructure.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

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
                command.rawPassword(),
                roles,
                passwordEncoder
        );

        repository.save(user);

        try {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    user.getCustomerId(),
                    user.getEmail().value()
            );

            String jsonPayload = objectMapper.writeValueAsString(event);

            OutboxEventJpaEntity outboxEvent = new OutboxEventJpaEntity(
                    "user.registered",
                    jsonPayload
            );

            outboxRepository.save(outboxEvent);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar o evento user.registered", e);
        }
    }
}