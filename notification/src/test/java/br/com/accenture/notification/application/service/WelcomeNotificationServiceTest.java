package br.com.accenture.notification.application.service;

import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.domain.enums.NotificationStatus;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import br.com.accenture.notification.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WelcomeNotificationServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final WelcomeNotificationService service = new WelcomeNotificationService(repository, emailSender);

    @Test
    void sendWelcomePersistsSentNotificationWhenEmailDeliverySucceeds() {
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.sendWelcome(TestFixtures.RECIPIENT);

        verify(emailSender).sendWelcomeEmail(TestFixtures.RECIPIENT);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification persisted = captor.getValue();
        assertThat(persisted.getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(persisted.getSubject()).isEqualTo(WelcomeNotificationService.WELCOME_SUBJECT);
        assertThat(persisted.getBody()).isEqualTo(WelcomeNotificationService.WELCOME_BODY);
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(persisted.getSentAt()).isNotNull();
    }

    @Test
    void sendWelcomePersistsFailedNotificationWhenEmailSenderThrows() {
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("smtp down")).when(emailSender).sendWelcomeEmail(TestFixtures.RECIPIENT);

        service.sendWelcome(TestFixtures.RECIPIENT);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification persisted = captor.getValue();
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(persisted.getSentAt()).isNull();
    }
}
