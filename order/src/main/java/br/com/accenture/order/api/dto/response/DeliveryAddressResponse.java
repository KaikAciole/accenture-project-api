package br.com.accenture.order.api.dto.response;

import br.com.accenture.order.domain.model.DeliveryAddress;

public record DeliveryAddressResponse(
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode
) {

    public static DeliveryAddressResponse from(DeliveryAddress address) {
        if (address == null) {
            return null;
        }
        return new DeliveryAddressResponse(
                address.street(),
                address.number(),
                address.complement(),
                address.neighborhood(),
                address.city(),
                address.state(),
                address.zipCode()
        );
    }
}
