package br.com.accenture.auth.application.usecase;

import br.com.accenture.auth.application.command.RegisterCommand;
import br.com.accenture.auth.application.exception.UserAlreadyExistsException;
import br.com.accenture.auth.domain.enums.Role;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.repository.UserCredentialRepository;
import br.com.accenture.auth.domain.service.PasswordEncoder;
import br.com.accenture.auth.domain.vo.Email;
import br.com.accenture.auth.infrastructure.messaging.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

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

        String mensagemJson = String.format("{\"customerId\": \"%s\", \"email\": \"%s\"}",
                newCredential.getCustomerId(), newCredential.getEmail().value());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                mensagemJson
        );

        log.info("Mensagem enviada com sucesso ao RabbitMQ! Novo usuario: {}", newCredential.getEmail().value());
    }
}