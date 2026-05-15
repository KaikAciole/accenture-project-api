package br.com.accenture.inventory.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductAvailabilityItemRequest(

        @NotBlank(message = "SKU is required")
        String sku,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be greater than zero")
        Integer quantity
) {
}
