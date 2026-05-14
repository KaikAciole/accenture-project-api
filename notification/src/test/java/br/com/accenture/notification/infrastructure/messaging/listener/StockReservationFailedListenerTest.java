package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.StockNotificationService;
import br.com.accenture.notification.infrastructure.messaging.event.StockReservationFailedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class StockReservationFailedListenerTest {

    private final StockNotificationService stockNotificationService = mock(StockNotificationService.class);
    private final StockReservationFailedListener listener = new StockReservationFailedListener(stockNotificationService);

    @Test
    void handleDelegatesEventToStockNotificationService() {
        StockReservationFailedEvent event = new StockReservationFailedEvent("order-1", "customer-1",
                "PROD-1", 2, "Estoque insuficiente");

        listener.handle(event);

        verify(stockNotificationService).notifyStockReservationFailed(event);
        verifyNoMoreInteractions(stockNotificationService);
    }
}
