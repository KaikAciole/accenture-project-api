package br.com.accenture.order.application.dto.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID customerId,
        BigDecimal totalAmount,
        List<ItemEvent> items
) {
    public record ItemEvent(String sku, int quantity) {}
}