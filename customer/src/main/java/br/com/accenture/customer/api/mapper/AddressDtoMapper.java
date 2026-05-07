package br.com.accenture.customer.api.mapper;

import br.com.accenture.customer.api.dto.AddressRequest;
import br.com.accenture.customer.api.dto.AddressResponse;
import br.com.accenture.customer.domain.model.Address;

import java.util.UUID;

public final class AddressDtoMapper {

    private AddressDtoMapper() {
    }

    public static Address toDomain(AddressRequest request, UUID customerId) {
        if (request == null) {
            return null;
        }
        return Address.createNew(
                customerId,
                request.street(),
                request.number(),
                request.complement(),
                request.neighborhood(),
                request.city(),
                request.state(),
                request.zipCode()
        );
    }

    public static AddressResponse toResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(
                address.getId(),
                address.getCustomerId(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getNeighborhood(),
                address.getCity(),
                address.getState(),
                address.getZipCode(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }

}
