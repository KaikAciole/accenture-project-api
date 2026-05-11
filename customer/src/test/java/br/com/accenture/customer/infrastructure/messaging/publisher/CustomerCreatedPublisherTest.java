package br.com.accenture.customer.infrastructure.messaging.publisher;

import br.com.accenture.customer.domain.event.CustomerCreatedEvent;
import br.com.accenture.customer.infrastructure.messaging.RabbitMqConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomerCreatedPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CustomerCreatedPublisher publisher;

    @Test
    void shouldPublishEventWithCorrectExchangeAndRoutingKey() {
        CustomerCreatedEvent event = new CustomerCreatedEvent(
                UUID.randomUUID(),
                "customer.created",
                Instant.now(),
                UUID.randomUUID(),
                "maria@example.com"
        );

        publisher.onCustomerCreated(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMqConfig.CUSTOMER_EVENTS_EXCHANGE,
                "customer.created",
                event
        );
    }

    @Test
    void shouldSwallowAmqpExceptionAndNotPropagate() {
        CustomerCreatedEvent event = CustomerCreatedEvent.of(UUID.randomUUID(), "maria@example.com");
        doThrow(new AmqpException("broker unreachable"))
                .when(rabbitTemplate).convertAndSend(
                        RabbitMqConfig.CUSTOMER_EVENTS_EXCHANGE,
                        "customer.created",
                        event
                );

        assertThatCode(() -> publisher.onCustomerCreated(event)).doesNotThrowAnyException();
    }

}
