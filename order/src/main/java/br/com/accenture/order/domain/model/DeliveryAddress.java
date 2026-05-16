package br.com.accenture.order.domain.model;

public record DeliveryAddress(
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode
) {

    public DeliveryAddress {
        requireNotBlank(street, "street");
        requireNotBlank(number, "number");
        requireNotBlank(neighborhood, "neighborhood");
        requireNotBlank(city, "city");
        requireNotBlank(state, "state");
        requireNotBlank(zipCode, "zipCode");
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
