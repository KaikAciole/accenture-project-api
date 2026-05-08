package br.com.accenture.notification.infrastructure.messaging.event;

public record EmailNotificationEvent(
        String recipient,
        String subject,
        String body
) {
}
