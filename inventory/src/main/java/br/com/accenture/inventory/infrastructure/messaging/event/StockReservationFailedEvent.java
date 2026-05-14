package br.com.accenture.inventory.infrastructure.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record StockReservationFailedEvent(
        UUID eventId,
        UUID orderId,
        String sku,
        Integer quantity,
        String reason,
        Instant occurredAt
) {
}