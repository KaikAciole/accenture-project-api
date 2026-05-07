package br.com.accenture.customer.domain.model;

public record CepLookupResult(
        String zipCode,
        String street,
        String complement,
        String neighborhood,
        String city,
        String state
) {
}
