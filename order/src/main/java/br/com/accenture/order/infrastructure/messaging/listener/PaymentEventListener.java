package br.com.accenture.order.infrastructure.messaging.listener;

import br.com.accenture.order.application.service.OrderService;
import br.com.accenture.order.infrastructure.messaging.dto.PaymentApprovedEvent;
import br.com.accenture.order.infrastructure.messaging.dto.PaymentCanceledEvent;
import br.com.accenture.order.infrastructure.messaging.dto.PaymentRefundedEvent;
import br.com.accenture.order.infrastructure.messaging.dto.PaymentRefusedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = "order.payment.approved.queue")
    public void handlePaymentApproved(PaymentApprovedEvent event) {
        orderService.markOrderAsPaid(event.orderId());
    }

    @RabbitListener(queues = "order.payment.refused.queue")
    public void handlePaymentRefused(PaymentRefusedEvent event) {
        orderService.cancelOrder(event.orderId(), "Payment refused: " + event.failureReason());
    }

    @RabbitListener(queues = "order.payment.canceled.queue")
    public void handlePaymentCanceled(PaymentCanceledEvent event) {
        orderService.cancelOrder(event.orderId(), "Payment canceled: " + event.cancellationReason());
    }

    @RabbitListener(queues = "order.payment.refunded.queue")
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        orderService.refundOrder(event.orderId(), "Payment refunded: " + event.reason());
    }
}