package br.com.accenture.order.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(
        @NotNull(message = "Customer ID is required")
        UUID customerId,

        @NotEmpty(message = "Order must have at least one item")
        @Valid
        List<OrderItemRequest> items
) {
}