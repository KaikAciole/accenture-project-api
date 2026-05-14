package br.com.accenture.notification.infrastructure.persistence;

import br.com.accenture.notification.domain.enums.NotificationStatus;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.infrastructure.config.JpaConfig;
import br.com.accenture.notification.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaConfig.class, NotificationRepositoryAdapter.class})
class NotificationRepositoryAdapterIntegrationTest {

    @Autowired
    private NotificationRepositoryAdapter adapter;

    @Test
    void saveNewNotificationReturnsDomainWithGeneratedIdAndCreatedAt() {
        Notification notification = Notification.create(
                TestFixtures.CUSTOMER_ID,
                TestFixtures.RECIPIENT,
                TestFixtures.SUBJECT,
                TestFixtures.BODY
        );

        Notification saved = adapter.save(notification);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.getSentAt()).isNull();
        assertThat(saved.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(saved.getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(saved.getSubject()).isEqualTo(TestFixtures.SUBJECT);
        assertThat(saved.getBody()).isEqualTo(TestFixtures.BODY);
    }

    @Test
    void findByIdReturnsPersistedDomainNotification() {
        Notification saved = adapter.save(Notification.create(
                TestFixtures.CUSTOMER_ID,
                TestFixtures.RECIPIENT,
                TestFixtures.SUBJECT,
                TestFixtures.BODY
        ));

        Optional<Notification> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(found.get().getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(found.get().getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void findByIdReturnsEmptyOptionalWhenNotificationDoesNotExist() {
        Optional<Notification> found = adapter.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void findFirstByCustomerIdReturnsOldestNotificationForThatCustomer() {
        Notification first = adapter.save(Notification.create(
                TestFixtures.CUSTOMER_ID,
                TestFixtures.RECIPIENT,
                "Welcome",
                "Welcome body"
        ));
        adapter.save(Notification.create(
                TestFixtures.CUSTOMER_ID,
                TestFixtures.RECIPIENT,
                "Order recebido",
                "Order body"
        ));

        Optional<Notification> found = adapter.findFirstByCustomerId(TestFixtures.CUSTOMER_ID);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(first.getId());
        assertThat(found.get().getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
    }

    @Test
    void findFirstByCustomerIdReturnsEmptyWhenCustomerHasNoNotifications() {
        Optional<Notification> found = adapter.findFirstByCustomerId("nonexistent-customer");

        assertThat(found).isEmpty();
    }

    @Test
    void persistsSentNotificationPreservingStatusAndSentAt() {
        Instant sentAt = Instant.parse("2026-05-08T16:00:00Z");
        Notification sent = Notification.restore(
                null,
                TestFixtures.CUSTOMER_ID,
                TestFixtures.RECIPIENT,
                TestFixtures.SUBJECT,
                TestFixtures.BODY,
                null,
                sentAt,
                NotificationStatus.SENT
        );

        Notification saved = adapter.save(sent);
        Notification reloaded = adapter.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(reloaded.getSentAt()).isEqualTo(sentAt);
    }
}
