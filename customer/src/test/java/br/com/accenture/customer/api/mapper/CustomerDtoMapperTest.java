package br.com.accenture.customer.api.mapper;

import br.com.accenture.customer.api.dto.CustomerRequest;
import br.com.accenture.customer.api.dto.CustomerResponse;
import br.com.accenture.customer.domain.model.Customer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerDtoMapperTest {

    @Test
    void toDomain_shouldMapAllFields() {
        CustomerRequest request = new CustomerRequest(
                "Maria", "maria@example.com", "12345678901", "secret123", "11999998888"
        );

        Customer customer = CustomerDtoMapper.toDomain(request);

        assertThat(customer.getId()).isNull();
        assertThat(customer.getName()).isEqualTo("Maria");
        assertThat(customer.getEmail()).isEqualTo("maria@example.com");
        assertThat(customer.getCpf()).isEqualTo("12345678901");
        assertThat(customer.getPassword()).isEqualTo("secret123");
        assertThat(customer.getPhone()).isEqualTo("11999998888");
    }

    @Test
    void toDomain_shouldReturnNullForNullRequest() {
        assertThat(CustomerDtoMapper.toDomain(null)).isNull();
    }

    @Test
    void toResponse_shouldMapAllFieldsExceptPassword() {
        UUID id = UUID.randomUUID();
        Instant created = Instant.parse("2024-01-01T10:00:00Z");
        Instant updated = Instant.parse("2024-01-02T10:00:00Z");
        Customer customer = Customer.restore(
                id, "Maria", "maria@example.com", "12345678901", "secret123", "11999998888", created, updated
        );

        CustomerResponse response = CustomerDtoMapper.toResponse(customer);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Maria");
        assertThat(response.email()).isEqualTo("maria@example.com");
        assertThat(response.cpf()).isEqualTo("12345678901");
        assertThat(response.phone()).isEqualTo("11999998888");
        assertThat(response.createdAt()).isEqualTo(created);
        assertThat(response.updatedAt()).isEqualTo(updated);
    }

    @Test
    void toResponse_shouldReturnNullForNullCustomer() {
        assertThat(CustomerDtoMapper.toResponse(null)).isNull();
    }

}
