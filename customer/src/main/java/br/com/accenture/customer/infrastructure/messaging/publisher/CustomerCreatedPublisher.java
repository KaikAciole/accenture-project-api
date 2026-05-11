package br.com.accenture.customer.infrastructure.messaging.publisher;

import br.com.accenture.customer.domain.event.CustomerCreatedEvent;
import br.com.accenture.customer.infrastructure.messaging.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CustomerCreatedPublisher {

    private static final Logger log = LoggerFactory.getLogger(CustomerCreatedPublisher.class);
    private static final String ROUTING_KEY = "customer.created";

    private final RabbitTemplate rabbitTemplate;

    public CustomerCreatedPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCustomerCreated(CustomerCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.CUSTOMER_EVENTS_EXCHANGE, ROUTING_KEY, event);
            log.info("Published {} event for customerId={}", event.eventType(), event.customerId());
        } catch (AmqpException ex) {
            log.warn("Failed to publish {} for customerId={} — broker unreachable, event lost",
                    event.eventType(), event.customerId(), ex);
        }
    }

}
