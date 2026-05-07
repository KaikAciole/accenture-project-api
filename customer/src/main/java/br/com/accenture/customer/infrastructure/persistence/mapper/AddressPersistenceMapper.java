package br.com.accenture.customer.infrastructure.persistence.mapper;

import br.com.accenture.customer.domain.model.Address;
import br.com.accenture.customer.infrastructure.persistence.entity.AddressJpaEntity;
import br.com.accenture.customer.infrastructure.persistence.entity.CustomerJpaEntity;

public final class AddressPersistenceMapper {

    private AddressPersistenceMapper() {
    }

    public static AddressJpaEntity toEntity(Address address, CustomerJpaEntity customer) {
        if (address == null) {
            return null;
        }
        return AddressJpaEntity.builder()
                .id(address.getId())
                .customer(customer)
                .street(address.getStreet())
                .number(address.getNumber())
                .complement(address.getComplement())
                .neighborhood(address.getNeighborhood())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .build();
    }

    public static Address toDomain(AddressJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Address.restore(
                entity.getId(),
                entity.getCustomer().getId(),
                entity.getStreet(),
                entity.getNumber(),
                entity.getComplement(),
                entity.getNeighborhood(),
                entity.getCity(),
                entity.getState(),
                entity.getZipCode(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

}
