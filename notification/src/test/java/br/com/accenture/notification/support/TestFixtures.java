package br.com.accenture.notification.support;

import br.com.accenture.notification.domain.enums.NotificationStatus;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.infrastructure.persistence.entity.NotificationJpaEntity;

import java.time.Instant;
import java.util.UUID;

public final class TestFixtures {

    public static final UUID NOTIFICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final String RECIPIENT = "user@example.com";
    public static final String SUBJECT = "Order confirmed";
    public static final String BODY = "Your order #42 has been confirmed.";
    public static final Instant CREATED_AT = Instant.parse("2026-05-08T12:00:00Z");
    public static final Instant SENT_AT = Instant.parse("2026-05-08T12:00:05Z");

    private TestFixtures() {
    }

    public static Notification newPendingNotification() {
        return Notification.create(RECIPIENT, SUBJECT, BODY);
    }

    public static Notification restoredPendingNotification() {
        return Notification.restore(
                NOTIFICATION_ID,
                RECIPIENT,
                SUBJECT,
                BODY,
                CREATED_AT,
                null,
                NotificationStatus.PENDING
        );
    }

    public static Notification restoredSentNotification() {
        return Notification.restore(
                NOTIFICATION_ID,
                RECIPIENT,
                SUBJECT,
                BODY,
                CREATED_AT,
                SENT_AT,
                NotificationStatus.SENT
        );
    }

    public static Notification restoredFailedNotification() {
        return Notification.restore(
                NOTIFICATION_ID,
                RECIPIENT,
                SUBJECT,
                BODY,
                CREATED_AT,
                null,
                NotificationStatus.FAILED
        );
    }

    public static NotificationJpaEntity pendingJpaEntity() {
        return NotificationJpaEntity.builder()
                .id(NOTIFICATION_ID)
                .recipient(RECIPIENT)
                .subject(SUBJECT)
                .body(BODY)
                .createdAt(CREATED_AT)
                .sentAt(null)
                .status(NotificationStatus.PENDING)
                .build();
    }
}
