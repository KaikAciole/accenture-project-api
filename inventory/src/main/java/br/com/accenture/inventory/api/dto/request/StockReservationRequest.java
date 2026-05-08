package br.com.accenture.inventory.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockReservationRequest(

        @NotNull(message = "Order ID is required")
        UUID orderId,

        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "Reserved quantity is required")
        @Min(value = 1, message = "Reserved quantity must be greater than zero")
        Integer reservedQuantity
) {
}