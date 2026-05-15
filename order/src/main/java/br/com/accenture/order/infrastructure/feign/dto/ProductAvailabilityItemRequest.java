package br.com.accenture.order.infrastructure.feign.dto;

public record ProductAvailabilityItemRequest(
        String sku,
        Integer quantity
) {}