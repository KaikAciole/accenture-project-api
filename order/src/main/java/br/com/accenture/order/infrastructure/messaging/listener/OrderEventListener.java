package br.com.accenture.order.infrastructure.messaging.listener;

import br.com.accenture.order.application.service.OrderService;
import br.com.accenture.order.domain.enums.OrderStatus;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.application.publisher.OrderEventPublisher;
import br.com.accenture.order.infrastructure.messaging.dto.PaymentApprovedEvent;
import br.com.accenture.order.infrastructure.messaging.dto.PaymentCanceledEvent;
import br.com.accenture.order.infrastructure.messaging.dto.PaymentRefundedEvent;
import br.com.accenture.order.infrastructure.messaging.dto.PaymentRefusedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventListener {

    private final OrderService orderService;
    private final OrderEventPublisher eventPublisher;

    public OrderEventListener(OrderService orderService, OrderEventPublisher eventPublisher) {
        this.orderService = orderService;
        this.eventPublisher = eventPublisher;
    }

    @RabbitListener(queues = "order.payment.approved.queue")
    public void handlePaymentApproved(PaymentApprovedEvent event) {
        Order order = orderService.findById(event.orderId());

        if (order.getStatus() == OrderStatus.CANCELED) {
            log.error("Pagamento recebido para pedido {} que ja esta CANCELADO. Solicitando estorno...", order.getId());
            eventPublisher.publishOrderCanceledEvent(order, "Pagamento recebido apos cancelamento por falta de estoque.");
            return;
        }

        try {
            orderService.markOrderAsPaid(order.getId());
        } catch (IllegalStateException e) {
            log.warn("Aguardando reserva de estoque para processar pagamento do pedido {}", order.getId());
            throw e;
        }
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