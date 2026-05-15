package br.com.accenture.inventory.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ProductAvailabilityRequest(

        @NotEmpty(message = "Items are required")
        List<@Valid ProductAvailabilityItemRequest> items
) {
}
