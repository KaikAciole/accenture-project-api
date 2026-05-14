package br.com.accenture.inventory.infrastructure.messaging.publisher;

import br.com.accenture.inventory.infrastructure.config.InventoryStockMessagingProperties;
import br.com.accenture.inventory.infrastructure.messaging.event.StockReservationCanceledEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.StockReservationConfirmedEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.StockReservationFailedEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.StockReservedEvent;
import br.com.accenture.inventory.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitStockEventPublisherTest {

    private final CapturingRabbitTemplate rabbitTemplate = new CapturingRabbitTemplate();
    private final RabbitStockEventPublisher publisher = new RabbitStockEventPublisher(
            rabbitTemplate,
            new InventoryStockMessagingProperties(
                    "stock.exchange",
                    new InventoryStockMessagingProperties.RoutingKey(
                            "stock.reserved",
                            "stock.reservation.failed",
                            "stock.reservation.confirmed",
                            "stock.reservation.canceled"
                    )
            )
    );

    @Test
    void publishStockReservedSendsReservedEvent() {
        publisher.publishStockReserved(TestFixtures.activeReservation());

        assertThat(rabbitTemplate.messages)
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.exchange()).isEqualTo("stock.exchange");
                    assertThat(message.routingKey()).isEqualTo("stock.reserved");
                    assertThat(message.payload()).isInstanceOf(StockReservedEvent.class);
                    StockReservedEvent event = (StockReservedEvent) message.payload();
                    assertThat(event.reservationId()).isEqualTo(TestFixtures.RESERVATION_ID);
                    assertThat(event.orderId()).isEqualTo(TestFixtures.ORDER_ID);
                    assertThat(event.sku()).isEqualTo("SKU-001");
                    assertThat(event.quantity()).isEqualTo(3);
                    assertThat(event.eventId()).isNotNull();
                    assertThat(event.occurredAt()).isNotNull();
                });
    }

    @Test
    void publishStockReservationConfirmedSendsConfirmedEvent() {
        publisher.publishStockReservationConfirmed(TestFixtures.confirmedReservation());

        assertThat(rabbitTemplate.messages)
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.exchange()).isEqualTo("stock.exchange");
                    assertThat(message.routingKey()).isEqualTo("stock.reservation.confirmed");
                    assertThat(message.payload()).isInstanceOf(StockReservationConfirmedEvent.class);
                    StockReservationConfirmedEvent event = (StockReservationConfirmedEvent) message.payload();
                    assertThat(event.reservationId()).isEqualTo(TestFixtures.RESERVATION_ID);
                    assertThat(event.orderId()).isEqualTo(TestFixtures.ORDER_ID);
                    assertThat(event.sku()).isEqualTo("SKU-001");
                    assertThat(event.quantity()).isEqualTo(3);
                    assertThat(event.eventId()).isNotNull();
                    assertThat(event.occurredAt()).isNotNull();
                });
    }

    @Test
    void publishStockReservationCanceledSendsCanceledEvent() {
        publisher.publishStockReservationCanceled(TestFixtures.canceledReservation());

        assertThat(rabbitTemplate.messages)
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.exchange()).isEqualTo("stock.exchange");
                    assertThat(message.routingKey()).isEqualTo("stock.reservation.canceled");
                    assertThat(message.payload()).isInstanceOf(StockReservationCanceledEvent.class);
                    StockReservationCanceledEvent event = (StockReservationCanceledEvent) message.payload();
                    assertThat(event.reservationId()).isEqualTo(TestFixtures.RESERVATION_ID);
                    assertThat(event.orderId()).isEqualTo(TestFixtures.ORDER_ID);
                    assertThat(event.sku()).isEqualTo("SKU-001");
                    assertThat(event.quantity()).isEqualTo(3);
                    assertThat(event.eventId()).isNotNull();
                    assertThat(event.occurredAt()).isNotNull();
                });
    }

    @Test
    void publishStockReservationFailedSendsFailedEvent() {
        publisher.publishStockReservationFailed(TestFixtures.ORDER_ID, "SKU-001", 3, "Insufficient stock");

        assertThat(rabbitTemplate.messages)
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.exchange()).isEqualTo("stock.exchange");
                    assertThat(message.routingKey()).isEqualTo("stock.reservation.failed");
                    assertThat(message.payload()).isInstanceOf(StockReservationFailedEvent.class);
                    StockReservationFailedEvent event = (StockReservationFailedEvent) message.payload();
                    assertThat(event.orderId()).isEqualTo(TestFixtures.ORDER_ID);
                    assertThat(event.sku()).isEqualTo("SKU-001");
                    assertThat(event.quantity()).isEqualTo(3);
                    assertThat(event.reason()).isEqualTo("Insufficient stock");
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
