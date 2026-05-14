package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.OrderNotificationService;
import br.com.accenture.notification.infrastructure.config.RabbitConfig;
import br.com.accenture.notification.infrastructure.messaging.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderPaidListener {

    private final OrderNotificationService orderNotificationService;

    public OrderPaidListener(OrderNotificationService orderNotificationService) {
        this.orderNotificationService = orderNotificationService;
    }

    @RabbitListener(queues = RabbitConfig.ORDER_PAID_QUEUE)
    public void handle(OrderPaidEvent event) {
        log.info("Received order.paid event for orderId={} customerId={}", event.orderId(), event.customerId());
        orderNotificationService.notifyOrderPaid(event);
    }
}
