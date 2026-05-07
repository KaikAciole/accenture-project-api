package br.com.accenture.customer.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        UUID customerId,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode,
        Instant createdAt,
        Instant updatedAt
) {
}
