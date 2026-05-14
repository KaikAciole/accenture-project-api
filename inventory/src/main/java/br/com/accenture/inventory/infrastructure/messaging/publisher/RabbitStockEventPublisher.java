package br.com.accenture.inventory.infrastructure.messaging.publisher;

import br.com.accenture.inventory.application.port.StockEventPublisher;
import br.com.accenture.inventory.domain.model.StockReservation;
import br.com.accenture.inventory.infrastructure.config.InventoryStockMessagingProperties;
import br.com.accenture.inventory.infrastructure.messaging.event.StockReservationCanceledEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.StockReservationConfirmedEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.StockReservationFailedEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.StockReservedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class RabbitStockEventPublisher implements StockEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final InventoryStockMessagingProperties properties;

    public RabbitStockEventPublisher(
            RabbitTemplate rabbitTemplate,
            InventoryStockMessagingProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publishStockReserved(StockReservation reservation) {
        StockReservedEvent event = new StockReservedEvent(
                UUID.randomUUID(),
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getProduct().getSku(),
                reservation.getReservedQuantity(),
                Instant.now()
        );

        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.routingKey().reserved(),
                event
        );
    }

    @Override
    public void publishStockReservationConfirmed(StockReservation reservation) {
        StockReservationConfirmedEvent event = new StockReservationConfirmedEvent(
                UUID.randomUUID(),
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getProduct().getSku(),
                reservation.getReservedQuantity(),
                Instant.now()
        );

        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.routingKey().reservationConfirmed(),
                event
        );
    }

    @Override
    public void publishStockReservationCanceled(StockReservation reservation) {
        StockReservationCanceledEvent event = new StockReservationCanceledEvent(
                UUID.randomUUID(),
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getProduct().getSku(),
                reservation.getReservedQuantity(),
                Instant.now()
        );

        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.routingKey().reservationCanceled(),
                event
        );
    }

    @Override
    public void publishStockReservationFailed(UUID orderId, String sku, Integer quantity, String reason) {
        StockReservationFailedEvent event = new StockReservationFailedEvent(
                UUID.randomUUID(),
                orderId,
                sku,
                quantity,
                reason,
                Instant.now()
        );

        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.routingKey().reservationFailed(),
                event
        );
    }
}