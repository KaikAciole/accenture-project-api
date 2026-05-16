package br.com.accenture.auth.infrastructure.messaging;

import br.com.accenture.auth.application.event.PasswordResetRequestedEvent;
import br.com.accenture.auth.application.event.UserRegisteredEvent;
import br.com.accenture.auth.application.publisher.AuthEventPublisher;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.infrastructure.persistence.entity.OutboxEventJpaEntity;
import br.com.accenture.auth.infrastructure.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthEventPublisherAdapter implements AuthEventPublisher {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publishUserRegisteredEvent(UserCredential user) {
        try {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    user.getCustomerId(),
                    user.getEmail().value()
            );

            String jsonPayload = objectMapper.writeValueAsString(event);

            OutboxEventJpaEntity outboxEntity = OutboxEventJpaEntity.create(
                    "User",
                    user.getCustomerId().toString(),
                    "user.registered",
                    jsonPayload
            );

            outboxRepository.save(outboxEntity);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Falha ao serializar evento de registro de usuário", e);
        }
    }

    @Override
    public void publishPasswordResetRequestedEvent(UUID customerId, String email, String plainToken) {
        try {
            PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(customerId, email, plainToken);

            String jsonPayload = objectMapper.writeValueAsString(event);

            OutboxEventJpaEntity outboxEntity = OutboxEventJpaEntity.create(
                    "User",
                    customerId.toString(),
                    "password.reset.requested",
                    jsonPayload
            );

            outboxRepository.save(outboxEntity);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Falha ao serializar evento de password reset", e);
        }
    }
}