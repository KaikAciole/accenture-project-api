package br.com.accenture.order.infrastructure.feign.dto;

public record ProductAvailabilityItemResponse(
        String sku,
        Integer requestedQuantity,
        Integer availableQuantity,
        boolean available
) {}