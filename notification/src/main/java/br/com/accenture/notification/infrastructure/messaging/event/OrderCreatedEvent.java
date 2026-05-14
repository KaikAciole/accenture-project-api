package br.com.accenture.notification.infrastructure.messaging.event;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedEvent(
        String orderId,
        String customerId,
        BigDecimal totalAmount,
        List<Item> items
) {
    public record Item(String sku, int quantity) {
    }
}
