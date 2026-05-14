package br.com.accenture.notification.infrastructure.persistence.mapper;

import br.com.accenture.notification.domain.enums.NotificationStatus;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import br.com.accenture.notification.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPersistenceMapperTest {

    private final NotificationPersistenceMapper mapper = new NotificationPersistenceMapper();

    @Test
    void toEntityMapsAllFieldsFromSentDomain() {
        Notification notification = TestFixtures.restoredSentNotification();

        NotificationJpaEntity entity = mapper.toEntity(notification);

        assertThat(entity.getId()).isEqualTo(TestFixtures.NOTIFICATION_ID);
        assertThat(entity.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(entity.getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(entity.getSubject()).isEqualTo(TestFixtures.SUBJECT);
        assertThat(entity.getBody()).isEqualTo(TestFixtures.BODY);
        assertThat(entity.getCreatedAt()).isEqualTo(TestFixtures.CREATED_AT);
        assertThat(entity.getSentAt()).isEqualTo(TestFixtures.SENT_AT);
        assertThat(entity.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void toDomainMapsAllFieldsFromEntity() {
        NotificationJpaEntity entity = TestFixtures.pendingJpaEntity();

        Notification notification = mapper.toDomain(entity);

        assertThat(notification.getId()).isEqualTo(TestFixtures.NOTIFICATION_ID);
        assertThat(notification.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(notification.getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(notification.getSubject()).isEqualTo(TestFixtures.SUBJECT);
        assertThat(notification.getBody()).isEqualTo(TestFixtures.BODY);
        assertThat(notification.getCreatedAt()).isEqualTo(TestFixtures.CREATED_AT);
        assertThat(notification.getSentAt()).isNull();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void toEntityReturnsNullWhenDomainIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDomainReturnsNullWhenEntityIsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }
}
