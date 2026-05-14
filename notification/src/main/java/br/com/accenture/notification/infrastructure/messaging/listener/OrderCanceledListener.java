package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.OrderNotificationService;
import br.com.accenture.notification.infrastructure.config.RabbitConfig;
import br.com.accenture.notification.infrastructure.messaging.event.OrderCanceledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderCanceledListener {

    private final OrderNotificationService orderNotificationService;

    public OrderCanceledListener(OrderNotificationService orderNotificationService) {
        this.orderNotificationService = orderNotificationService;
    }

    @RabbitListener(queues = RabbitConfig.ORDER_CANCELED_QUEUE)
    public void handle(OrderCanceledEvent event) {
        log.info("Received order.canceled event for orderId={} customerId={}", event.orderId(), event.customerId());
        orderNotificationService.notifyOrderCanceled(event);
    }
}
