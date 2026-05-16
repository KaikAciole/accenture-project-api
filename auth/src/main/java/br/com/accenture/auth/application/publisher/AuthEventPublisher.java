package br.com.accenture.auth.application.publisher;

import br.com.accenture.auth.domain.model.UserCredential;

import java.util.UUID;

public interface AuthEventPublisher {
    void publishUserRegisteredEvent(UserCredential user);
    void publishPasswordResetRequestedEvent(UUID customerId, String email, String plainToken);
}