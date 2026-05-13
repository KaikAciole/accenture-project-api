package br.com.accenture.notification.api.mapper;

import br.com.accenture.notification.api.dto.NotificationResponse;
import br.com.accenture.notification.domain.enums.NotificationStatus;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDtoMapperTest {

    @Test
    void toResponseMapsAllFieldsForSentNotification() {
        Notification notification = TestFixtures.restoredSentNotification();

        NotificationResponse response = NotificationDtoMapper.toResponse(notification);

        assertThat(response.id()).isEqualTo(TestFixtures.NOTIFICATION_ID);
        assertThat(response.customerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(response.recipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(response.subject()).isEqualTo(TestFixtures.SUBJECT);
        assertThat(response.body()).isEqualTo(TestFixtures.BODY);
        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.createdAt()).isEqualTo(TestFixtures.CREATED_AT);
        assertThat(response.sentAt()).isEqualTo(TestFixtures.SENT_AT);
    }

    @Test
    void toResponseMapsPendingNotificationWithNullSentAt() {
        Notification notification = TestFixtures.restoredPendingNotification();

        NotificationResponse response = NotificationDtoMapper.toResponse(notification);

        assertThat(response.status()).isEqualTo(NotificationStatus.PENDING);
        assertThat(response.sentAt()).isNull();
    }

    @Test
    void toResponseReturnsNullWhenNotificationIsNull() {
        assertThat(NotificationDtoMapper.toResponse(null)).isNull();
    }
}
