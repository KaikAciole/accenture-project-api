package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.OrderNotificationService;
import br.com.accenture.notification.infrastructure.messaging.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class OrderCreatedListenerTest {

    private final OrderNotificationService orderNotificationService = mock(OrderNotificationService.class);
    private final OrderCreatedListener listener = new OrderCreatedListener(orderNotificationService);

    @Test
    void handleDelegatesEventToOrderNotificationService() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                "order-1",
                "customer-1",
                new BigDecimal("99.90"),
                List.of(new OrderCreatedEvent.Item("SKU-1", 1))
        );

        listener.handle(event);

        verify(orderNotificationService).notifyOrderCreated(event);
        verifyNoMoreInteractions(orderNotificationService);
    }
}
