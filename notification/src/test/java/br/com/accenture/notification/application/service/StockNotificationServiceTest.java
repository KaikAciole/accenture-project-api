package br.com.accenture.notification.application.service;

import br.com.accenture.notification.application.port.CustomerLookup;
import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.domain.enums.NotificationStatus;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import br.com.accenture.notification.infrastructure.messaging.event.StockReservationFailedEvent;
import br.com.accenture.notification.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockNotificationServiceTest {

    private static final String ORDER_ID = "order-42";
    private static final String SKU = "PROD-999";
    private static final int QUANTITY = 3;
    private static final String REASON = "Estoque insuficiente";

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final CustomerLookup customerLookup = mock(CustomerLookup.class);
    private final StockNotificationService service = new StockNotificationService(repository, emailSender, customerLookup);

    @Test
    void notifyStockReservationFailedPersistsSentNotificationAndSendsEmail() {
        StockReservationFailedEvent event = new StockReservationFailedEvent(ORDER_ID, TestFixtures.CUSTOMER_ID,
                SKU, QUANTITY, REASON);
        when(customerLookup.findEmailByCustomerId(TestFixtures.CUSTOMER_ID))
                .thenReturn(Optional.of(TestFixtures.RECIPIENT));
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.notifyStockReservationFailed(event);

        verify(emailSender).sendStockReservationFailedEmail(TestFixtures.RECIPIENT, ORDER_ID, SKU, QUANTITY, REASON);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification persisted = captor.getValue();
        assertThat(persisted.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(persisted.getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(persisted.getSubject()).isEqualTo(StockNotificationService.STOCK_RESERVATION_FAILED_SUBJECT);
        assertThat(persisted.getBody()).contains(ORDER_ID).contains(SKU).contains(REASON);
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void notifyStockReservationFailedPropagatesAndDoesNotPersistWhenEmailSenderThrows() {
        StockReservationFailedEvent event = new StockReservationFailedEvent(ORDER_ID, TestFixtures.CUSTOMER_ID,
                SKU, QUANTITY, REASON);
        when(customerLookup.findEmailByCustomerId(TestFixtures.CUSTOMER_ID))
                .thenReturn(Optional.of(TestFixtures.RECIPIENT));
        doThrow(new RuntimeException("smtp down"))
                .when(emailSender).sendStockReservationFailedEmail(eq(TestFixtures.RECIPIENT), any(), any(), anyInt(), any());

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> service.notifyStockReservationFailed(event))
                .withMessage("smtp down");
        verify(repository, never()).save(any());
    }

    @Test
    void notifyStockReservationFailedSkipsWhenCustomerNotFound() {
        StockReservationFailedEvent event = new StockReservationFailedEvent(ORDER_ID, "unknown-customer",
                SKU, QUANTITY, REASON);
        when(customerLookup.findEmailByCustomerId("unknown-customer")).thenReturn(Optional.empty());

        service.notifyStockReservationFailed(event);

        verify(emailSender, never()).sendStockReservationFailedEmail(any(), any(), any(), anyInt(), any());
        verify(repository, never()).save(any());
    }
}
