package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.OrderNotificationService;
import br.com.accenture.notification.infrastructure.messaging.event.OrderCanceledEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class OrderCanceledListenerTest {

    private final OrderNotificationService orderNotificationService = mock(OrderNotificationService.class);
    private final OrderCanceledListener listener = new OrderCanceledListener(orderNotificationService);

    @Test
    void handleDelegatesEventToOrderNotificationService() {
        OrderCanceledEvent event = new OrderCanceledEvent("order-1", "customer-1", "Pagamento recusado");

        listener.handle(event);

        verify(orderNotificationService).notifyOrderCanceled(event);
        verifyNoMoreInteractions(orderNotificationService);
    }
}
