package br.com.accenture.order.api.dto.request;

import br.com.accenture.order.application.dto.OrderItemCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank(message = "SKU cannot be blank")
        String sku,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        Integer quantity,

        @NotNull(message = "Unit price is required")
        @PositiveOrZero(message = "Unit price must be zero or greater")
        BigDecimal unitPrice
) {

    public OrderItemCommand toCommand() {
        return new OrderItemCommand(sku, quantity, unitPrice);
    }
}