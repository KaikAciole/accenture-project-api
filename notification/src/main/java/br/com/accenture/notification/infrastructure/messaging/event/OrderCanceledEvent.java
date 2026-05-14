package br.com.accenture.notification.infrastructure.messaging.event;

public record OrderCanceledEvent(
        String orderId,
        String customerId,
        String reason
) {
}
