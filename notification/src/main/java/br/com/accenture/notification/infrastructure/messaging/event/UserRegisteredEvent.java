package br.com.accenture.notification.infrastructure.messaging.event;

public record UserRegisteredEvent(
        String customerId,
        String email
) {
}
