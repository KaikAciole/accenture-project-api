package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.StockNotificationService;
import br.com.accenture.notification.infrastructure.config.RabbitConfig;
import br.com.accenture.notification.infrastructure.messaging.event.StockReservationFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockReservationFailedListener {

    private final StockNotificationService stockNotificationService;

    public StockReservationFailedListener(StockNotificationService stockNotificationService) {
        this.stockNotificationService = stockNotificationService;
    }

    @RabbitListener(queues = RabbitConfig.STOCK_RESERVATION_FAILED_QUEUE)
    public void handle(StockReservationFailedEvent event) {
        log.info("Received stock.reservation.failed event for orderId={} customerId={} sku={}",
                event.orderId(), event.customerId(), event.sku());
        stockNotificationService.notifyStockReservationFailed(event);
    }
}
