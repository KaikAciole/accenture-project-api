package br.com.accenture.customer.api.dto;

public record CepLookupResponse(
        String zipCode,
        String street,
        String complement,
        String neighborhood,
        String city,
        String state
) {
}
