package br.com.accenture.order.infrastructure.messaging.listener;

import br.com.accenture.order.application.dto.event.PaymentEvent;
import br.com.accenture.order.application.dto.event.StockFailedEvent;
import br.com.accenture.order.application.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private final OrderService orderService;

    public OrderEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = "order.queue.payment-approved")
    public void handlePaymentApproved(PaymentEvent event) {
        log.info("Recebido evento de pagamento aprovado para o pedido: {}", event.orderId());
        orderService.markOrderAsPaid(event.orderId());
    }

    @RabbitListener(queues = "order.queue.payment-failed")
    public void handlePaymentFailed(PaymentEvent event) {
        log.warn("Recebido evento de falha no pagamento para o pedido: {}", event.orderId());

        String reason = event.failureReason() != null ? event.failureReason() : event.cancellationReason();
        if (reason == null) reason = "Pagamento rejeitado pelo gateway.";

        orderService.cancelOrder(event.orderId(), reason);
    }

    @RabbitListener(queues = "order.queue.stock-failed")
    public void handleStockFailed(StockFailedEvent event) {
        log.error("Recebido evento de falha no estoque para o pedido: {}. SKU: {}", event.orderId(), event.sku());
        orderService.cancelOrder(event.orderId(), "Falta de estoque para o item: " + event.sku());
    }
}