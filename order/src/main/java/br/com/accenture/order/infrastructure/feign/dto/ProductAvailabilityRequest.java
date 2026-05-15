package br.com.accenture.order.infrastructure.feign.dto;

import java.util.List;

public record ProductAvailabilityRequest(
        List<ProductAvailabilityItemRequest> items
) {}