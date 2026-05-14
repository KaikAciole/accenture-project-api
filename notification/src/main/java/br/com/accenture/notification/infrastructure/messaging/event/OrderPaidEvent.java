package br.com.accenture.notification.infrastructure.messaging.event;

public record OrderPaidEvent(
        String orderId,
        String customerId
) {
}
