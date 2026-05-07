package br.com.accenture.order.application.dto;

import java.math.BigDecimal;

public record OrderItemCommand(
        String sku,
        Integer quantity,
        BigDecimal unitPrice
) {
}