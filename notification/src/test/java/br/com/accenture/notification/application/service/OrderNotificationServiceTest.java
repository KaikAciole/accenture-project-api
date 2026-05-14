package br.com.accenture.notification.application.service;

import br.com.accenture.notification.application.port.CustomerLookup;
import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.application.port.data.OrderCreatedEmailData;
import br.com.accenture.notification.domain.enums.NotificationStatus;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import br.com.accenture.notification.infrastructure.messaging.event.OrderCanceledEvent;
import br.com.accenture.notification.infrastructure.messaging.event.OrderCreatedEvent;
import br.com.accenture.notification.infrastructure.messaging.event.OrderPaidEvent;
import br.com.accenture.notification.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
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

class OrderNotificationServiceTest {

    private static final String ORDER_ID = "order-42";
    private static final String REASON = "Pagamento recusado";

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final CustomerLookup customerLookup = mock(CustomerLookup.class);
    private final OrderNotificationService service = new OrderNotificationService(repository, emailSender, customerLookup);

    @Test
    void notifyOrderCreatedPersistsSentNotificationAndSendsEmailWithItems() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                ORDER_ID,
                TestFixtures.CUSTOMER_ID,
                new BigDecimal("300.00"),
                List.of(new OrderCreatedEvent.Item("PROD-999", 2))
        );
        when(customerLookup.findEmailByCustomerId(TestFixtures.CUSTOMER_ID))
                .thenReturn(Optional.of(TestFixtures.RECIPIENT));
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.notifyOrderCreated(event);

        ArgumentCaptor<OrderCreatedEmailData> dataCaptor = ArgumentCaptor.forClass(OrderCreatedEmailData.class);
        verify(emailSender).sendOrderCreatedEmail(eq(TestFixtures.RECIPIENT), dataCaptor.capture());
        OrderCreatedEmailData data = dataCaptor.getValue();
        assertThat(data.orderId()).isEqualTo(ORDER_ID);
        assertThat(data.totalAmount()).isEqualByComparingTo("300.00");
        assertThat(data.items()).hasSize(1);
        assertThat(data.items().getFirst().sku()).isEqualTo("PROD-999");
        assertThat(data.items().getFirst().quantity()).isEqualTo(2);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification persisted = captor.getValue();
        assertThat(persisted.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(persisted.getRecipient()).isEqualTo(TestFixtures.RECIPIENT);
        assertThat(persisted.getSubject()).isEqualTo(OrderNotificationService.ORDER_CREATED_SUBJECT);
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void notifyOrderPaidPersistsSentNotificationAndSendsEmail() {
        OrderPaidEvent event = new OrderPaidEvent(ORDER_ID, TestFixtures.CUSTOMER_ID);
        when(customerLookup.findEmailByCustomerId(TestFixtures.CUSTOMER_ID))
                .thenReturn(Optional.of(TestFixtures.RECIPIENT));
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.notifyOrderPaid(event);

        verify(emailSender).sendOrderPaidEmail(TestFixtures.RECIPIENT, ORDER_ID);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification persisted = captor.getValue();
        assertThat(persisted.getSubject()).isEqualTo(OrderNotificationService.ORDER_PAID_SUBJECT);
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void notifyOrderCanceledPersistsSentNotificationAndSendsEmailWithReason() {
        OrderCanceledEvent event = new OrderCanceledEvent(ORDER_ID, TestFixtures.CUSTOMER_ID, REASON);
        when(customerLookup.findEmailByCustomerId(TestFixtures.CUSTOMER_ID))
                .thenReturn(Optional.of(TestFixtures.RECIPIENT));
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.notifyOrderCanceled(event);

        verify(emailSender).sendOrderCanceledEmail(TestFixtures.RECIPIENT, ORDER_ID, REASON);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification persisted = captor.getValue();
        assertThat(persisted.getSubject()).isEqualTo(OrderNotificationService.ORDER_CANCELED_SUBJECT);
        assertThat(persisted.getBody()).contains(REASON);
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void notifyOrderCreatedPropagatesAndDoesNotPersistWhenEmailSenderThrows() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                ORDER_ID,
                TestFixtures.CUSTOMER_ID,
                new BigDecimal("300.00"),
                List.of(new OrderCreatedEvent.Item("PROD-999", 2))
        );
        when(customerLookup.findEmailByCustomerId(TestFixtures.CUSTOMER_ID))
                .thenReturn(Optional.of(TestFixtures.RECIPIENT));
        doThrow(new RuntimeException("smtp down"))
                .when(emailSender).sendOrderCreatedEmail(eq(TestFixtures.RECIPIENT), any());

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> service.notifyOrderCreated(event))
                .withMessage("smtp down");
        verify(repository, never()).save(any());
    }

    @Test
    void notifyOrderCreatedSkipsWhenCustomerNotFound() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                ORDER_ID,
                "unknown-customer",
                new BigDecimal("300.00"),
                List.of(new OrderCreatedEvent.Item("PROD-999", 2))
        );
        when(customerLookup.findEmailByCustomerId("unknown-customer")).thenReturn(Optional.empty());

        service.notifyOrderCreated(event);

        verify(emailSender, never()).sendOrderCreatedEmail(any(), any());
        verify(repository, never()).save(any());
    }
}
