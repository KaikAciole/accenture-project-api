package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.OrderNotificationService;
import br.com.accenture.notification.infrastructure.messaging.event.OrderPaidEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class OrderPaidListenerTest {

    private final OrderNotificationService orderNotificationService = mock(OrderNotificationService.class);
    private final OrderPaidListener listener = new OrderPaidListener(orderNotificationService);

    @Test
    void handleDelegatesEventToOrderNotificationService() {
        OrderPaidEvent event = new OrderPaidEvent("order-1", "customer-1");

        listener.handle(event);

        verify(orderNotificationService).notifyOrderPaid(event);
        verifyNoMoreInteractions(orderNotificationService);
    }
}
