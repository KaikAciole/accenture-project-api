package br.com.accenture.payment.infrastructure.messaging.publisher;

import br.com.accenture.payment.infrastructure.config.PaymentMessagingProperties;
import br.com.accenture.payment.infrastructure.messaging.event.PaymentApprovedEvent;
import br.com.accenture.payment.infrastructure.messaging.event.PaymentCanceledEvent;
import br.com.accenture.payment.infrastructure.messaging.event.PaymentRefusedEvent;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitPaymentEventPublisherTest {

    private final CapturingRabbitTemplate rabbitTemplate = new CapturingRabbitTemplate();
    private final RabbitPaymentEventPublisher publisher = new RabbitPaymentEventPublisher(
            rabbitTemplate,
            new PaymentMessagingProperties(
                    "payment.events",
                    new PaymentMessagingProperties.RoutingKey(
                            "payment.approved",
                            "payment.refused",
                            "payment.canceled"
                    )
            )
    );

    @Test
    void publishPaymentApprovedSendsApprovedEvent() {
        publisher.publishPaymentApproved(TestFixtures.approvedPayment());

        assertThat(rabbitTemplate.messages)
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.exchange()).isEqualTo("payment.events");
                    assertThat(message.routingKey()).isEqualTo("payment.approved");
                    assertThat(message.payload()).isInstanceOf(PaymentApprovedEvent.class);
                    PaymentApprovedEvent event = (PaymentApprovedEvent) message.payload();
                    assertThat(event.paymentId()).isEqualTo(TestFixtures.PAYMENT_ID);
                    assertThat(event.orderId()).isEqualTo(TestFixtures.ORDER_ID);
                    assertThat(event.customerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
                    assertThat(event.amount()).isEqualByComparingTo(TestFixtures.AMOUNT);
                    assertThat(event.method()).isEqualTo(TestFixtures.approvedPayment().getMethod());
                    assertThat(event.paidAt()).isEqualTo(TestFixtures.PAID_AT);
                    assertThat(event.eventId()).isNotNull();
                    assertThat(event.occurredAt()).isNotNull();
                });
    }

    @Test
    void publishPaymentRefusedSendsRefusedEvent() {
        publisher.publishPaymentRefused(TestFixtures.refusedPayment());

        assertThat(rabbitTemplate.messages)
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.exchange()).isEqualTo("payment.events");
                    assertThat(message.routingKey()).isEqualTo("payment.refused");
                    assertThat(message.payload()).isInstanceOf(PaymentRefusedEvent.class);
                    PaymentRefusedEvent event = (PaymentRefusedEvent) message.payload();
                    assertThat(event.paymentId()).isEqualTo(TestFixtures.PAYMENT_ID);
                    assertThat(event.failureReason()).isEqualTo(TestFixtures.FAILURE_REASON);
                    assertThat(event.eventId()).isNotNull();
                    assertThat(event.occurredAt()).isNotNull();
                });
    }

    @Test
    void publishPaymentCanceledSendsCanceledEvent() {
        publisher.publishPaymentCanceled(TestFixtures.canceledPayment());

        assertThat(rabbitTemplate.messages)
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.exchange()).isEqualTo("payment.events");
                    assertThat(message.routingKey()).isEqualTo("payment.canceled");
                    assertThat(message.payload()).isInstanceOf(PaymentCanceledEvent.class);
                    PaymentCanceledEvent event = (PaymentCanceledEvent) message.payload();
                    assertThat(event.paymentId()).isEqualTo(TestFixtures.PAYMENT_ID);
                    assertThat(event.cancellationReason()).isEqualTo("Customer requested");
                    assertThat(event.eventId()).isNotNull();
                    assertThat(event.occurredAt()).isNotNull();
                });
    }

    private record SentMessage(String exchange, String routingKey, Object payload) {
    }

    private static final class CapturingRabbitTemplate extends RabbitTemplate {

        private final List<SentMessage> messages = new ArrayList<>();

        @Override
        public void convertAndSend(String exchange, String routingKey, Object object) {
            messages.add(new SentMessage(exchange, routingKey, object));
        }
    }
}
