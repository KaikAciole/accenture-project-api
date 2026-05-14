package br.com.accenture.inventory.infrastructure.messaging.listener;

import br.com.accenture.inventory.application.port.StockEventPublisher;
import br.com.accenture.inventory.application.service.StockReservationService;
import br.com.accenture.inventory.infrastructure.messaging.event.OrderCanceledEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.OrderCreatedEvent;
import br.com.accenture.inventory.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OrderEventListenerTest {

    private final StockReservationService stockReservationService = mock(StockReservationService.class);
    private final StockEventPublisher stockEventPublisher = mock(StockEventPublisher.class);
    private final OrderEventListener listener = new OrderEventListener(stockReservationService, stockEventPublisher);

    @Test
    void handleOrderCreatedDelegatesEachItemToStockReservationService() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                TestFixtures.ORDER_ID,
                TestFixtures.CUSTOMER_ID.toString(),
                new BigDecimal("200.00"),
                List.of(
                        new OrderCreatedEvent.OrderItemEvent("SKU-001", 2),
                        new OrderCreatedEvent.OrderItemEvent("SKU-002", 1)
                )
        );

        listener.handleOrderCreated(event);

        verify(stockReservationService).createBySku(TestFixtures.ORDER_ID, "SKU-001", 2);
        verify(stockReservationService).createBySku(TestFixtures.ORDER_ID, "SKU-002", 1);
        verifyNoInteractions(stockEventPublisher);
    }

    @Test
    void handleOrderCreatedPublishesFailureWhenReservationFails() {
        RuntimeException exception = new RuntimeException("Insufficient stock");
        doThrow(exception)
                .when(stockReservationService)
                .createBySku(TestFixtures.ORDER_ID, "SKU-001", 2);
        OrderCreatedEvent event = new OrderCreatedEvent(
                TestFixtures.ORDER_ID,
                TestFixtures.CUSTOMER_ID.toString(),
                new BigDecimal("200.00"),
                List.of(new OrderCreatedEvent.OrderItemEvent("SKU-001", 2))
        );

        listener.handleOrderCreated(event);

        verify(stockEventPublisher).publishStockReservationFailed(
                TestFixtures.ORDER_ID,
                "SKU-001",
                2,
                "Insufficient stock"
        );
    }

    @Test
    void handleOrderCanceledDelegatesToStockReservationService() {
        listener.handleOrderCanceled(new OrderCanceledEvent(
                TestFixtures.ORDER_ID,
                TestFixtures.CUSTOMER_ID.toString(),
                "Order canceled"
        ));

        verify(stockReservationService).cancelByOrderId(TestFixtures.ORDER_ID);
        verifyNoInteractions(stockEventPublisher);
    }
}
