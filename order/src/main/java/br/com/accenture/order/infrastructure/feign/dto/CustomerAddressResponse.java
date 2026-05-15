package br.com.accenture.order.infrastructure.feign.dto;

import java.util.UUID;

public record CustomerAddressResponse(
        UUID id,
        UUID customerId,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode
) {
}
