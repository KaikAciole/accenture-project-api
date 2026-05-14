package br.com.accenture.notification.application.service;

import br.com.accenture.notification.application.port.CustomerLookup;
import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.domain.enums.NotificationStatus;
import br.com.accenture.notification.domain.enums.PaymentMethod;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import br.com.accenture.notification.infrastructure.messaging.event.PaymentCanceledEvent;
import br.com.accenture.notification.infrastructure.messaging.event.PaymentRefusedEvent;
import br.com.accenture.notification.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentNotificationServiceTest {

    private static final String PAYMENT_ID = "payment-7";
    private static final String ORDER_ID = "order-42";
    private static final BigDecimal AMOUNT = new BigDecimal("250.00");
    private static final String FAILURE_REASON = "Cartao sem limite";
    private static final String CANCELLATION_REASON = "Cancelado pelo cliente";

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final CustomerLookup customerLookup = mock(CustomerLookup.class);
    private final PaymentNotificationService service = new PaymentNotificationService(repository, emailSender, customerLookup);

    @Test
    void notifyPaymentRefusedPersistsSentNotificationAndSendsEmail() {
        PaymentRefusedEvent event = new PaymentRefusedEvent(PAYMENT_ID, ORDER_ID, TestFixtures.CUSTOMER_ID,
                AMOUNT, PaymentMethod.CREDIT_CARD, FAILURE_REASON);
        when(customerLookup.findEmailByCustomerId(TestFixtures.CUSTOMER_ID))
                .thenReturn(Optional.of(TestFixtures.RECIPIENT));
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.notifyPaymentRefused(event);

        verify(emailSender).sendPaymentRefusedEmail(TestFixtures.RECIPIENT, ORDER_ID, AMOUNT,
                PaymentMethod.CREDIT_CARD, FAILURE_REASON);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification persisted = captor.getValue();
        assertThat(persisted.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(persisted.getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(persisted.getSubject()).isEqualTo(PaymentNotificationService.PAYMENT_REFUSED_SUBJECT);
        assertThat(persisted.getBody()).contains(ORDER_ID).contains(FAILURE_REASON);
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void notifyPaymentCanceledPersistsSentNotificationAndSendsEmail() {
        PaymentCanceledEvent event = new PaymentCanceledEvent(PAYMENT_ID, ORDER_ID, TestFixtures.CUSTOMER_ID,
                AMOUNT, PaymentMethod.PIX, CANCELLATION_REASON);
        when(customerLookup.findEmailByCustomerId(TestFixtures.CUSTOMER_ID))
                .thenReturn(Optional.of(TestFixtures.RECIPIENT));
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.notifyPaymentCanceled(event);

        verify(emailSender).sendPaymentCanceledEmail(TestFixtures.RECIPIENT, ORDER_ID, AMOUNT,
                PaymentMethod.PIX, CANCELLATION_REASON);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification persisted = captor.getValue();
        assertThat(persisted.getSubject()).isEqualTo(PaymentNotificationService.PAYMENT_CANCELED_SUBJECT);
        assertThat(persisted.getBody()).contains(ORDER_ID).contains(CANCELLATION_REASON);
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void notifyPaymentRefusedPropagatesAndDoesNotPersistWhenEmailSenderThrows() {
        PaymentRefusedEvent event = new PaymentRefusedEvent(PAYMENT_ID, ORDER_ID, TestFixtures.CUSTOMER_ID,
                AMOUNT, PaymentMethod.CREDIT_CARD, FAILURE_REASON);
        when(customerLookup.findEmailByCustomerId(TestFixtures.CUSTOMER_ID))
                .thenReturn(Optional.of(TestFixtures.RECIPIENT));
        doThrow(new RuntimeException("smtp down"))
                .when(emailSender).sendPaymentRefusedEmail(eq(TestFixtures.RECIPIENT), any(), any(), any(), any());

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> service.notifyPaymentRefused(event))
                .withMessage("smtp down");
        verify(repository, never()).save(any());
    }

    @Test
    void notifyPaymentRefusedSkipsWhenCustomerNotFound() {
        PaymentRefusedEvent event = new PaymentRefusedEvent(PAYMENT_ID, ORDER_ID, "unknown-customer",
                AMOUNT, PaymentMethod.CREDIT_CARD, FAILURE_REASON);
        when(customerLookup.findEmailByCustomerId("unknown-customer")).thenReturn(Optional.empty());

        service.notifyPaymentRefused(event);

        verify(emailSender, never()).sendPaymentRefusedEmail(any(), any(), any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void notifyPaymentCanceledSkipsWhenCustomerNotFound() {
        PaymentCanceledEvent event = new PaymentCanceledEvent(PAYMENT_ID, ORDER_ID, "unknown-customer",
                AMOUNT, PaymentMethod.PIX, CANCELLATION_REASON);
        when(customerLookup.findEmailByCustomerId("unknown-customer")).thenReturn(Optional.empty());

        service.notifyPaymentCanceled(event);

        verify(emailSender, never()).sendPaymentCanceledEmail(any(), any(), any(), any(), any());
        verify(repository, never()).save(any());
    }
}
