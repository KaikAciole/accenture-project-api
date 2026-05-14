package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.OrderNotificationService;
import br.com.accenture.notification.infrastructure.config.RabbitConfig;
import br.com.accenture.notification.infrastructure.messaging.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderCreatedListener {

    private final OrderNotificationService orderNotificationService;

    public OrderCreatedListener(OrderNotificationService orderNotificationService) {
        this.orderNotificationService = orderNotificationService;
    }

    @RabbitListener(queues = RabbitConfig.ORDER_CREATED_QUEUE)
    public void handle(OrderCreatedEvent event) {
        log.info("Received order.created event for orderId={} customerId={}", event.orderId(), event.customerId());
        orderNotificationService.notifyOrderCreated(event);
    }
}
