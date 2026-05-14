package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.PaymentNotificationService;
import br.com.accenture.notification.infrastructure.config.RabbitConfig;
import br.com.accenture.notification.infrastructure.messaging.event.PaymentRefusedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentRefusedListener {

    private final PaymentNotificationService paymentNotificationService;

    public PaymentRefusedListener(PaymentNotificationService paymentNotificationService) {
        this.paymentNotificationService = paymentNotificationService;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_REFUSED_QUEUE)
    public void handle(PaymentRefusedEvent event) {
        log.info("Received payment.refused event for paymentId={} orderId={} customerId={}",
                event.paymentId(), event.orderId(), event.customerId());
        paymentNotificationService.notifyPaymentRefused(event);
    }
}
