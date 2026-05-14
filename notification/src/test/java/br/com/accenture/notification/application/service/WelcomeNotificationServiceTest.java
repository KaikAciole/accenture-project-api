package br.com.accenture.notification.application.service;

import br.com.accenture.notification.application.port.CustomerLookup;
import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.domain.enums.NotificationStatus;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import br.com.accenture.notification.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WelcomeNotificationServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final CustomerLookup customerLookup = mock(CustomerLookup.class);
    private final WelcomeNotificationService service = new WelcomeNotificationService(repository, emailSender, customerLookup);

    @Test
    void sendWelcomePersistsSentNotificationWhenEmailDeliverySucceeds() {
        when(customerLookup.findEmailByCustomerId(TestFixtures.CUSTOMER_ID))
                .thenReturn(Optional.of(TestFixtures.RECIPIENT));
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.sendWelcome(TestFixtures.CUSTOMER_ID);

        verify(emailSender).sendWelcomeEmail(TestFixtures.RECIPIENT);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification persisted = captor.getValue();
        assertThat(persisted.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(persisted.getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(persisted.getSubject()).isEqualTo(WelcomeNotificationService.WELCOME_SUBJECT);
        assertThat(persisted.getBody()).isEqualTo(WelcomeNotificationService.WELCOME_BODY);
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(persisted.getSentAt()).isNotNull();
    }

    @Test
    void sendWelcomePropagatesAndDoesNotPersistWhenEmailSenderThrows() {
        when(customerLookup.findEmailByCustomerId(TestFixtures.CUSTOMER_ID))
                .thenReturn(Optional.of(TestFixtures.RECIPIENT));
        doThrow(new RuntimeException("smtp down")).when(emailSender).sendWelcomeEmail(TestFixtures.RECIPIENT);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> service.sendWelcome(TestFixtures.CUSTOMER_ID))
                .withMessage("smtp down");
        verify(repository, never()).save(any());
    }

    @Test
    void sendWelcomeSkipsWhenCustomerNotFound() {
        when(customerLookup.findEmailByCustomerId("unknown-customer")).thenReturn(Optional.empty());

        service.sendWelcome("unknown-customer");

        verify(emailSender, never()).sendWelcomeEmail(any());
        verify(repository, never()).save(any());
    }
}
