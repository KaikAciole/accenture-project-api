package br.com.accenture.notification.domain.model;

import br.com.accenture.notification.domain.enums.NotificationStatus;
import br.com.accenture.notification.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class NotificationTest {

    @Test
    void createBuildsPendingNotificationWithoutIdAndTimestamps() {
        Notification notification = Notification.create(
                TestFixtures.RECIPIENT,
                TestFixtures.SUBJECT,
                TestFixtures.BODY
        );

        assertThat(notification.getId()).isNull();
        assertThat(notification.getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(notification.getSubject()).isEqualTo(TestFixtures.SUBJECT);
        assertThat(notification.getBody()).isEqualTo(TestFixtures.BODY);
        assertThat(notification.getCreatedAt()).isNull();
        assertThat(notification.getSentAt()).isNull();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void createRejectsBlankRecipient() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Notification.create(null, TestFixtures.SUBJECT, TestFixtures.BODY))
                .withMessage("recipient must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Notification.create(" ", TestFixtures.SUBJECT, TestFixtures.BODY))
                .withMessage("recipient must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Notification.create("", TestFixtures.SUBJECT, TestFixtures.BODY))
                .withMessage("recipient must not be blank");
    }

    @Test
    void createRejectsBlankSubject() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Notification.create(TestFixtures.RECIPIENT, null, TestFixtures.BODY))
                .withMessage("subject must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Notification.create(TestFixtures.RECIPIENT, "  ", TestFixtures.BODY))
                .withMessage("subject must not be blank");
    }

    @Test
    void createRejectsBlankBody() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Notification.create(TestFixtures.RECIPIENT, TestFixtures.SUBJECT, null))
                .withMessage("body must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Notification.create(TestFixtures.RECIPIENT, TestFixtures.SUBJECT, ""))
                .withMessage("body must not be blank");
    }

    @Test
    void restoreReconstructsAllFieldsWithoutValidation() {
        Notification notification = Notification.restore(
                TestFixtures.NOTIFICATION_ID,
                TestFixtures.RECIPIENT,
                TestFixtures.SUBJECT,
                TestFixtures.BODY,
                TestFixtures.CREATED_AT,
                TestFixtures.SENT_AT,
                NotificationStatus.SENT
        );

        assertThat(notification.getId()).isEqualTo(TestFixtures.NOTIFICATION_ID);
        assertThat(notification.getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(notification.getSubject()).isEqualTo(TestFixtures.SUBJECT);
        assertThat(notification.getBody()).isEqualTo(TestFixtures.BODY);
        assertThat(notification.getCreatedAt()).isEqualTo(TestFixtures.CREATED_AT);
        assertThat(notification.getSentAt()).isEqualTo(TestFixtures.SENT_AT);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void markAsSentTransitionsFromPendingAndSetsSentAt() {
        Notification notification = TestFixtures.newPendingNotification();
        Instant before = Instant.now();

        notification.markAsSent();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
        assertThat(notification.getSentAt()).isAfterOrEqualTo(before);
    }

    @Test
    void markAsSentRejectsTransitionFromSent() {
        Notification notification = TestFixtures.restoredSentNotification();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(notification::markAsSent)
                .withMessage("Cannot mark as sent from current status: SENT");
    }

    @Test
    void markAsSentRejectsTransitionFromFailed() {
        Notification notification = TestFixtures.restoredFailedNotification();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(notification::markAsSent)
                .withMessage("Cannot mark as sent from current status: FAILED");
    }

    @Test
    void markAsFailedTransitionsFromPendingAndKeepsSentAtNull() {
        Notification notification = TestFixtures.newPendingNotification();

        notification.markAsFailed();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getSentAt()).isNull();
    }

    @Test
    void markAsFailedRejectsTransitionFromSent() {
        Notification notification = TestFixtures.restoredSentNotification();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(notification::markAsFailed)
                .withMessage("Cannot mark as failed from current status: SENT");
    }

    @Test
    void markAsFailedRejectsTransitionFromFailed() {
        Notification notification = TestFixtures.restoredFailedNotification();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(notification::markAsFailed)
                .withMessage("Cannot mark as failed from current status: FAILED");
    }
}
