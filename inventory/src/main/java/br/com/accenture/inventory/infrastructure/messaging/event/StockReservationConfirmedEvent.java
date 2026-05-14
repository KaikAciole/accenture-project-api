package br.com.accenture.inventory.infrastructure.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record StockReservationConfirmedEvent(
        UUID eventId,
        UUID reservationId,
        UUID orderId,
        String sku,
        Integer quantity,
        Instant occurredAt
) {
}