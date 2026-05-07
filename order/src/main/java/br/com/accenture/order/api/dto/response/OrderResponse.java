package br.com.accenture.order.api.dto.response;

import br.com.accenture.order.domain.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        String customerId,
        String status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        Instant createdAt
) {

    public OrderResponse(Order order) {
        this(
                order.getId(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::new).toList(),
                order.getCreatedAt()
        );
    }
}