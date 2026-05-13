package br.com.accenture.payment.infrastructure.messaging.publisher;

import br.com.accenture.payment.application.port.PaymentEventPublisher;
import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.infrastructure.config.PaymentMessagingProperties;
import br.com.accenture.payment.infrastructure.messaging.event.PaymentApprovedEvent;
import br.com.accenture.payment.infrastructure.messaging.event.PaymentCanceledEvent;
import br.com.accenture.payment.infrastructure.messaging.event.PaymentRefusedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class RabbitPaymentEventPublisher implements PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final PaymentMessagingProperties properties;

    public RabbitPaymentEventPublisher(
            RabbitTemplate rabbitTemplate,
            PaymentMessagingProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publishPaymentApproved(Payment payment) {
        PaymentApprovedEvent event = new PaymentApprovedEvent(
                UUID.randomUUID(),
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getPaidAt(),
                Instant.now()
        );

        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.routingKey().approved(),
                event
        );
    }

    @Override
    public void publishPaymentRefused(Payment payment) {
        PaymentRefusedEvent event = new PaymentRefusedEvent(
                UUID.randomUUID(),
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getFailureReason(),
                Instant.now()
        );

        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.routingKey().refused(),
                event
        );
    }

    @Override
    public void publishPaymentCanceled(Payment payment) {
        PaymentCanceledEvent event = new PaymentCanceledEvent(
                UUID.randomUUID(),
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getFailureReason(),
                Instant.now()
        );

        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.routingKey().canceled(),
                event
        );
    }
}