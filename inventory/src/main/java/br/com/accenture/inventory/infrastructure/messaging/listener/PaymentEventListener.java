package br.com.accenture.inventory.infrastructure.messaging.listener;

import br.com.accenture.inventory.application.service.StockReservationService;
import br.com.accenture.inventory.infrastructure.messaging.event.PaymentApprovedEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.PaymentCanceledEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.PaymentRefusedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private final StockReservationService stockReservationService;

    public PaymentEventListener(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @RabbitListener(queues = "${inventory.messaging.payment.queue.approved}")
    public void handlePaymentApproved(PaymentApprovedEvent event) {
        stockReservationService.confirmByOrderId(event.orderId());
    }

    @RabbitListener(queues = "${inventory.messaging.payment.queue.refused}")
    public void handlePaymentRefused(PaymentRefusedEvent event) {
        stockReservationService.cancelByOrderId(event.orderId());
    }

    @RabbitListener(queues = "${inventory.messaging.payment.queue.canceled}")
    public void handlePaymentCanceled(PaymentCanceledEvent event) {
        stockReservationService.cancelByOrderId(event.orderId());
    }
}