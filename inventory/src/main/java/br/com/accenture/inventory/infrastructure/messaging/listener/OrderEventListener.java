package br.com.accenture.inventory.infrastructure.messaging.listener;

import br.com.accenture.inventory.application.port.StockEventPublisher;
import br.com.accenture.inventory.application.service.StockReservationService;
import br.com.accenture.inventory.infrastructure.messaging.event.OrderCanceledEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.OrderCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private final StockReservationService stockReservationService;
    private final StockEventPublisher stockEventPublisher;

    public OrderEventListener(
            StockReservationService stockReservationService,
            StockEventPublisher stockEventPublisher
    ) {
        this.stockReservationService = stockReservationService;
        this.stockEventPublisher = stockEventPublisher;
    }

    @RabbitListener(queues = "${inventory.messaging.order.queue.created}")
    public void handleOrderCreated(OrderCreatedEvent event) {
        event.items().forEach(item -> {
            try {
                stockReservationService.createBySku(
                        event.orderId(),
                        item.sku(),
                        item.quantity()
                );
            } catch (RuntimeException exception) {
                stockEventPublisher.publishStockReservationFailed(
                        event.orderId(),
                        item.sku(),
                        item.quantity(),
                        exception.getMessage()
                );
            }
        });
    }

    @RabbitListener(queues = "${inventory.messaging.order.queue.canceled}")
    public void handleOrderCanceled(OrderCanceledEvent event) {
        stockReservationService.cancelByOrderId(event.orderId());
    }
}