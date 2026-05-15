package br.com.accenture.auth.application.publisher;

import br.com.accenture.auth.domain.model.UserCredential;

public interface AuthEventPublisher {
    void publishUserRegisteredEvent(UserCredential user);
}