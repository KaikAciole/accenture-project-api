package br.com.accenture.customer.api.mapper;

import br.com.accenture.customer.api.dto.AddressRequest;
import br.com.accenture.customer.api.dto.AddressResponse;
import br.com.accenture.customer.domain.model.Address;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AddressDtoMapperTest {

    @Test
    void toDomain_shouldMapAllFieldsAndCarryCustomerId() {
        UUID customerId = UUID.randomUUID();
        AddressRequest request = new AddressRequest(
                "Rua A", "100", "Apt 1", "Centro", "São Paulo", "SP", "01001000"
        );

        Address address = AddressDtoMapper.toDomain(request, customerId);

        assertThat(address.getId()).isNull();
        assertThat(address.getCustomerId()).isEqualTo(customerId);
        assertThat(address.getStreet()).isEqualTo("Rua A");
        assertThat(address.getNumber()).isEqualTo("100");
        assertThat(address.getComplement()).isEqualTo("Apt 1");
        assertThat(address.getNeighborhood()).isEqualTo("Centro");
        assertThat(address.getCity()).isEqualTo("São Paulo");
        assertThat(address.getState()).isEqualTo("SP");
        assertThat(address.getZipCode()).isEqualTo("01001000");
    }

    @Test
    void toDomain_shouldReturnNullWhenRequestIsNull() {
        assertThat(AddressDtoMapper.toDomain(null, UUID.randomUUID())).isNull();
    }

    @Test
    void toResponse_shouldMapAllFields() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Instant created = Instant.parse("2024-01-01T10:00:00Z");
        Instant updated = Instant.parse("2024-01-02T10:00:00Z");
        Address address = Address.restore(
                id, customerId, "Rua A", "100", "Apt 1", "Centro", "São Paulo", "SP", "01001000", created, updated
        );

        AddressResponse response = AddressDtoMapper.toResponse(address);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.street()).isEqualTo("Rua A");
        assertThat(response.number()).isEqualTo("100");
        assertThat(response.complement()).isEqualTo("Apt 1");
        assertThat(response.neighborhood()).isEqualTo("Centro");
        assertThat(response.city()).isEqualTo("São Paulo");
        assertThat(response.state()).isEqualTo("SP");
        assertThat(response.zipCode()).isEqualTo("01001000");
        assertThat(response.createdAt()).isEqualTo(created);
        assertThat(response.updatedAt()).isEqualTo(updated);
    }

    @Test
    void toResponse_shouldReturnNullWhenAddressIsNull() {
        assertThat(AddressDtoMapper.toResponse(null)).isNull();
    }

}
