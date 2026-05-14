package br.com.accenture.payment.infrastructure.messaging.listener;

import br.com.accenture.payment.application.service.payment.PaymentService;
import br.com.accenture.payment.infrastructure.messaging.event.OrderCanceledEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private final PaymentService paymentService;

    public OrderEventListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @RabbitListener(queues = "${payment.messaging.order.queue.canceled}")
    public void handleOrderCanceled(OrderCanceledEvent event) {
        paymentService.cancelByOrderId(event.orderId(), event.reason());
    }
}