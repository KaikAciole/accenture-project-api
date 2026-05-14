package br.com.accenture.inventory.infrastructure.messaging.listener;

import br.com.accenture.inventory.application.service.StockReservationService;
import br.com.accenture.inventory.infrastructure.messaging.event.OrderCanceledEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.OrderCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private final StockReservationService stockReservationService;

    public OrderEventListener(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @RabbitListener(queues = "${inventory.messaging.order.queue.created}")
    public void handleOrderCreated(OrderCreatedEvent event) {
        event.items().forEach(item ->
                stockReservationService.createBySku(
                        event.orderId(),
                        item.sku(),
                        item.quantity()
                )
        );
    }

    @RabbitListener(queues = "${inventory.messaging.order.queue.canceled}")
    public void handleOrderCanceled(OrderCanceledEvent event) {
        stockReservationService.cancelByOrderId(event.orderId());
    }
}