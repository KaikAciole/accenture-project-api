package br.com.accenture.notification.domain.model;

import br.com.accenture.notification.domain.enums.NotificationStatus;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Notification {

    private UUID id;
    private String recipient;
    private String subject;
    private String body;
    private Instant createdAt;
    private Instant sentAt;
    private NotificationStatus status;

    private Notification(UUID id,
                         String recipient,
                         String subject,
                         String body,
                         Instant createdAt,
                         Instant sentAt,
                         NotificationStatus status) {
        this.id = id;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
        this.status = status;
    }

    public static Notification create(String recipient, String subject, String body) {
        requireNotBlank(recipient, "recipient");
        requireNotBlank(subject, "subject");
        requireNotBlank(body, "body");
        return new Notification(
                null,
                recipient,
                subject,
                body,
                null,
                null,
                NotificationStatus.PENDING
        );
    }

    public static Notification restore(UUID id,
                                       String recipient,
                                       String subject,
                                       String body,
                                       Instant createdAt,
                                       Instant sentAt,
                                       NotificationStatus status) {
        return new Notification(id, recipient, subject, body, createdAt, sentAt, status);
    }

    public void markAsSent() {
        if (this.status != NotificationStatus.PENDING) {
            throw new IllegalStateException("Cannot mark as sent from current status: " + this.status);
        }
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markAsFailed() {
        if (this.status != NotificationStatus.PENDING) {
            throw new IllegalStateException("Cannot mark as failed from current status: " + this.status);
        }
        this.status = NotificationStatus.FAILED;
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

}
