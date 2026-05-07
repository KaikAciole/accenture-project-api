package br.com.accenture.customer.infrastructure.persistence.mapper;

import br.com.accenture.customer.domain.model.Address;
import br.com.accenture.customer.infrastructure.persistence.entity.AddressJpaEntity;
import br.com.accenture.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AddressPersistenceMapperTest {

    @Test
    void toEntity_shouldMapAllFieldsAndKeepCustomerReference() {
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Address address = Address.restore(
                addressId, customerId, "Rua A", "100", "Apt 1", "Centro", "São Paulo", "SP", "01001000",
                Instant.now(), Instant.now()
        );
        CustomerJpaEntity customerRef = CustomerJpaEntity.builder().id(customerId).build();

        AddressJpaEntity entity = AddressPersistenceMapper.toEntity(address, customerRef);

        assertThat(entity.getId()).isEqualTo(addressId);
        assertThat(entity.getCustomer()).isSameAs(customerRef);
        assertThat(entity.getStreet()).isEqualTo("Rua A");
        assertThat(entity.getNumber()).isEqualTo("100");
        assertThat(entity.getComplement()).isEqualTo("Apt 1");
        assertThat(entity.getNeighborhood()).isEqualTo("Centro");
        assertThat(entity.getCity()).isEqualTo("São Paulo");
        assertThat(entity.getState()).isEqualTo("SP");
        assertThat(entity.getZipCode()).isEqualTo("01001000");
    }

    @Test
    void toEntity_shouldReturnNullForNullAddress() {
        assertThat(AddressPersistenceMapper.toEntity(null, null)).isNull();
    }

    @Test
    void toDomain_shouldMapAllFieldsAndExposeCustomerId() {
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Instant created = Instant.parse("2024-01-01T10:00:00Z");
        Instant updated = Instant.parse("2024-01-02T10:00:00Z");
        CustomerJpaEntity customer = CustomerJpaEntity.builder().id(customerId).build();
        AddressJpaEntity entity = AddressJpaEntity.builder()
                .id(addressId)
                .customer(customer)
                .street("Rua A")
                .number("100")
                .complement("Apt 1")
                .neighborhood("Centro")
                .city("São Paulo")
                .state("SP")
                .zipCode("01001000")
                .createdAt(created)
                .updatedAt(updated)
                .build();

        Address address = AddressPersistenceMapper.toDomain(entity);

        assertThat(address.getId()).isEqualTo(addressId);
        assertThat(address.getCustomerId()).isEqualTo(customerId);
        assertThat(address.getStreet()).isEqualTo("Rua A");
        assertThat(address.getZipCode()).isEqualTo("01001000");
        assertThat(address.getCreatedAt()).isEqualTo(created);
        assertThat(address.getUpdatedAt()).isEqualTo(updated);
    }

    @Test
    void toDomain_shouldReturnNullForNullEntity() {
        assertThat(AddressPersistenceMapper.toDomain(null)).isNull();
    }

}
