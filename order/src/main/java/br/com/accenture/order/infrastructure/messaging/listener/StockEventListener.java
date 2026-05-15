package br.com.accenture.order.infrastructure.messaging.listener;

import br.com.accenture.order.application.service.OrderService;
import br.com.accenture.order.infrastructure.messaging.dto.StockEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = "order.stock.reserved.queue")
    public void handleStockReserved(StockEvents.Reserved event) {
        orderService.markOrderAsReserved(event.orderId());
    }

    @RabbitListener(queues = "order.stock.failed.queue")
    public void handleStockFailed(StockEvents.Failed event) {
        orderService.failOrderDueToStock(event.orderId(), "Stock reservation failed: " + event.reason());
    }
}