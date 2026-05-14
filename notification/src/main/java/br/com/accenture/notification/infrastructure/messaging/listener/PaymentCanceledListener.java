package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.PaymentNotificationService;
import br.com.accenture.notification.infrastructure.config.RabbitConfig;
import br.com.accenture.notification.infrastructure.messaging.event.PaymentCanceledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentCanceledListener {

    private final PaymentNotificationService paymentNotificationService;

    public PaymentCanceledListener(PaymentNotificationService paymentNotificationService) {
        this.paymentNotificationService = paymentNotificationService;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_CANCELED_QUEUE)
    public void handle(PaymentCanceledEvent event) {
        log.info("Received payment.canceled event for paymentId={} orderId={} customerId={}",
                event.paymentId(), event.orderId(), event.customerId());
        paymentNotificationService.notifyPaymentCanceled(event);
    }
}
