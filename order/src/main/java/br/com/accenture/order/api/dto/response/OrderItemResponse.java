package br.com.accenture.order.api.dto.response;

import br.com.accenture.order.domain.model.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID orderItemId,
        String sku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {

    public OrderItemResponse(OrderItem item) {
        this(
                item.getId(),
                item.getSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }
}