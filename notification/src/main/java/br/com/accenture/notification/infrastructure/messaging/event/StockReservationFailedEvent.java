package br.com.accenture.notification.infrastructure.messaging.event;

public record StockReservationFailedEvent(
        String orderId,
        String customerId,
        String sku,
        int quantity,
        String reason
) {
}
