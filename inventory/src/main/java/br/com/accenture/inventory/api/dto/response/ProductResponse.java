package br.com.accenture.inventory.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String category,
        BigDecimal basePrice,
        Integer stockQuantity
) {
}