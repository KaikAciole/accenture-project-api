package br.com.accenture.inventory.api.dto.response;

public record ProductAvailabilityItemResponse(
        String sku,
        Integer requestedQuantity,
        Integer availableQuantity,
        boolean available
) {
}
