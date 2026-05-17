package br.com.accenture.notification.application.service;

import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.domain.enums.NotificationStatus;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import br.com.accenture.notification.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetNotificationServiceTest {

    private static final String DEFAULT_RESET_URL = "http://localhost:5173/reset-password";
    private static final String RESET_URL_WITH_QUERY = "http://localhost:5173/reset?lang=pt";
    private static final UUID CUSTOMER_UUID = UUID.fromString(TestFixtures.CUSTOMER_ID);
    private static final String TOKEN = "abc123-token";

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final EmailSender emailSender = mock(EmailSender.class);

    private PasswordResetNotificationService serviceWith(String resetUrlBase) {
        return new PasswordResetNotificationService(repository, emailSender, resetUrlBase);
    }

    @Test
    void sendPasswordResetPersistsSentNotificationWhenEmailDeliverySucceeds() {
        PasswordResetNotificationService service = serviceWith(DEFAULT_RESET_URL);
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.sendPasswordReset(CUSTOMER_UUID, TestFixtures.RECIPIENT, TOKEN);

        verify(emailSender).sendPasswordResetEmail(eq(TestFixtures.RECIPIENT), any(String.class));
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification persisted = captor.getValue();
        assertThat(persisted.getCustomerId()).isEqualTo(CUSTOMER_UUID.toString());
        assertThat(persisted.getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(persisted.getSubject()).isEqualTo(PasswordResetNotificationService.PASSWORD_RESET_SUBJECT);
        assertThat(persisted.getBody()).isEqualTo(PasswordResetNotificationService.PASSWORD_RESET_BODY);
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(persisted.getSentAt()).isNotNull();
    }

    @Test
    void sendPasswordResetBuildsResetLinkWithQuestionMarkSeparatorWhenBaseUrlHasNoQueryString() {
        PasswordResetNotificationService service = serviceWith(DEFAULT_RESET_URL);

        service.sendPasswordReset(CUSTOMER_UUID, TestFixtures.RECIPIENT, TOKEN);

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendPasswordResetEmail(eq(TestFixtures.RECIPIENT), linkCaptor.capture());
        assertThat(linkCaptor.getValue())
                .isEqualTo(DEFAULT_RESET_URL + "?token=" + TOKEN + "&email=" + TestFixtures.RECIPIENT);
    }

    @Test
    void sendPasswordResetBuildsResetLinkWithAmpersandSeparatorWhenBaseUrlAlreadyHasQueryString() {
        PasswordResetNotificationService service = serviceWith(RESET_URL_WITH_QUERY);

        service.sendPasswordReset(CUSTOMER_UUID, TestFixtures.RECIPIENT, TOKEN);

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendPasswordResetEmail(eq(TestFixtures.RECIPIENT), linkCaptor.capture());
        assertThat(linkCaptor.getValue())
                .isEqualTo(RESET_URL_WITH_QUERY + "&token=" + TOKEN + "&email=" + TestFixtures.RECIPIENT);
    }

    @Test
    void sendPasswordResetPropagatesAndDoesNotPersistWhenEmailSenderThrows() {
        PasswordResetNotificationService service = serviceWith(DEFAULT_RESET_URL);
        doThrow(new RuntimeException("smtp down"))
                .when(emailSender).sendPasswordResetEmail(eq(TestFixtures.RECIPIENT), any(String.class));

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> service.sendPasswordReset(CUSTOMER_UUID, TestFixtures.RECIPIENT, TOKEN))
                .withMessage("smtp down");
        verify(repository, never()).save(any());
    }

    @Test
    void sendPasswordResetPropagatesWhenRepositorySaveFailsAfterEmailAlreadySent() {
        PasswordResetNotificationService service = serviceWith(DEFAULT_RESET_URL);
        doThrow(new RuntimeException("db unavailable"))
                .when(repository).save(any(Notification.class));

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> service.sendPasswordReset(CUSTOMER_UUID, TestFixtures.RECIPIENT, TOKEN))
                .withMessage("db unavailable");
        verify(emailSender).sendPasswordResetEmail(eq(TestFixtures.RECIPIENT), any(String.class));
    }
}
