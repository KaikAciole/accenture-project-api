package br.com.accenture.notification.application.service;

import br.com.accenture.notification.domain.exceptions.NotificationNotFoundException;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import br.com.accenture.notification.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationService service = new NotificationService(repository);

    @Test
    void createPersistsPendingNotificationAndReturnsSavedInstance() {
        Notification saved = TestFixtures.restoredPendingNotification();
        when(repository.save(any(Notification.class))).thenReturn(saved);

        Notification result = service.create(TestFixtures.RECIPIENT, TestFixtures.SUBJECT, TestFixtures.BODY);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification persisted = captor.getValue();
        assertThat(persisted.getId()).isNull();
        assertThat(persisted.getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(persisted.getSubject()).isEqualTo(TestFixtures.SUBJECT);
        assertThat(persisted.getBody()).isEqualTo(TestFixtures.BODY);
        assertThat(persisted.getStatus().name()).isEqualTo("PENDING");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void createPropagatesValidationErrorAndDoesNotPersist() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.create("  ", TestFixtures.SUBJECT, TestFixtures.BODY))
                .withMessage("recipient must not be blank");

        verify(repository, never()).save(any());
    }

    @Test
    void findByIdReturnsExistingNotification() {
        Notification stored = TestFixtures.restoredPendingNotification();
        when(repository.findById(TestFixtures.NOTIFICATION_ID)).thenReturn(Optional.of(stored));

        Notification result = service.findById(TestFixtures.NOTIFICATION_ID);

        assertThat(result).isSameAs(stored);
        verify(repository).findById(TestFixtures.NOTIFICATION_ID);
    }

    @Test
    void findByIdThrowsNotFoundWhenAbsent() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatExceptionOfType(NotificationNotFoundException.class)
                .isThrownBy(() -> service.findById(id))
                .withMessage("Notification not found: " + id);
    }
}
