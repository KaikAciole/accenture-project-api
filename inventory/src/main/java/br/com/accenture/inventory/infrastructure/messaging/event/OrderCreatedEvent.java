package br.com.accenture.inventory.infrastructure.messaging.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String customerId,
        BigDecimal totalAmount,
        List<OrderItemEvent> items
) {
    public record OrderItemEvent(
            String sku,
            Integer quantity
    ) {
    }
}