package br.com.accenture.notification.infrastructure.persistence;

import br.com.accenture.notification.domain.enums.NotificationStatus;
import br.com.accenture.notification.infrastructure.config.JpaConfig;
import br.com.accenture.notification.infrastructure.persistence.entity.NotificationJpaEntity;
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
@Import(JpaConfig.class)
class NotificationJpaRepositoryIntegrationTest {

    @Autowired
    private NotificationJpaRepository repository;

    @Test
    void saveGeneratesUuidAndCreatedAtViaAuditing() {
        Instant before = Instant.now();
        NotificationJpaEntity entity = NotificationJpaEntity.builder()
                .customerId(TestFixtures.CUSTOMER_ID)
                .recipient(TestFixtures.RECIPIENT)
                .subject(TestFixtures.SUBJECT)
                .body(TestFixtures.BODY)
                .status(NotificationStatus.PENDING)
                .build();

        NotificationJpaEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isAfterOrEqualTo(before);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.getSentAt()).isNull();
    }

    @Test
    void findByIdReturnsPersistedEntity() {
        NotificationJpaEntity saved = repository.save(NotificationJpaEntity.builder()
                .customerId(TestFixtures.CUSTOMER_ID)
                .recipient(TestFixtures.RECIPIENT)
                .subject(TestFixtures.SUBJECT)
                .body(TestFixtures.BODY)
                .status(NotificationStatus.PENDING)
                .build());

        Optional<NotificationJpaEntity> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(found.get().getSubject()).isEqualTo(TestFixtures.SUBJECT);
        assertThat(found.get().getBody()).isEqualTo(TestFixtures.BODY);
    }

    @Test
    void findByIdReturnsEmptyWhenAbsent() {
        Optional<NotificationJpaEntity> found = repository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void persistsSentStatusWithSentAtTimestamp() {
        Instant sentAt = Instant.parse("2026-05-08T15:30:00Z");
        NotificationJpaEntity saved = repository.save(NotificationJpaEntity.builder()
                .customerId(TestFixtures.CUSTOMER_ID)
                .recipient(TestFixtures.RECIPIENT)
                .subject(TestFixtures.SUBJECT)
                .body(TestFixtures.BODY)
                .status(NotificationStatus.SENT)
                .sentAt(sentAt)
                .build());

        NotificationJpaEntity reloaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(reloaded.getSentAt()).isEqualTo(sentAt);
    }
}
