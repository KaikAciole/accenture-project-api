package br.com.accenture.customer.infrastructure.persistence.mapper;

import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerPersistenceMapperTest {

    @Test
    void toEntity_shouldMapAllFields() {
        UUID id = UUID.randomUUID();
        Customer customer = Customer.restore(
                id, "Maria", "maria@example.com", "12345678901", "11999998888",
                Instant.now(), Instant.now()
        );

        CustomerJpaEntity entity = CustomerPersistenceMapper.toEntity(customer);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getName()).isEqualTo("Maria");
        assertThat(entity.getEmail()).isEqualTo("maria@example.com");
        assertThat(entity.getCpf()).isEqualTo("12345678901");
        assertThat(entity.getPhone()).isEqualTo("11999998888");
    }

    @Test
    void toEntity_shouldReturnNullForNullInput() {
        assertThat(CustomerPersistenceMapper.toEntity(null)).isNull();
    }

    @Test
    void toEntity_shouldMapMinimalCustomerWithOnlyEmail() {
        Customer customer = Customer.createMinimal("maria@example.com");

        CustomerJpaEntity entity = CustomerPersistenceMapper.toEntity(customer);

        assertThat(entity.getEmail()).isEqualTo("maria@example.com");
        assertThat(entity.getName()).isNull();
        assertThat(entity.getCpf()).isNull();
        assertThat(entity.getPhone()).isNull();
    }

    @Test
    void toDomain_shouldMapAllFieldsIncludingTimestamps() {
        UUID id = UUID.randomUUID();
        Instant created = Instant.parse("2024-01-01T10:00:00Z");
        Instant updated = Instant.parse("2024-01-02T10:00:00Z");
        CustomerJpaEntity entity = CustomerJpaEntity.builder()
                .id(id)
                .name("Maria")
                .email("maria@example.com")
                .cpf("12345678901")
                .phone("11999998888")
                .createdAt(created)
                .updatedAt(updated)
                .build();

        Customer customer = CustomerPersistenceMapper.toDomain(entity);

        assertThat(customer.getId()).isEqualTo(id);
        assertThat(customer.getName()).isEqualTo("Maria");
        assertThat(customer.getEmail()).isEqualTo("maria@example.com");
        assertThat(customer.getCpf()).isEqualTo("12345678901");
        assertThat(customer.getPhone()).isEqualTo("11999998888");
        assertThat(customer.getCreatedAt()).isEqualTo(created);
        assertThat(customer.getUpdatedAt()).isEqualTo(updated);
    }

    @Test
    void toDomain_shouldReturnNullForNullInput() {
        assertThat(CustomerPersistenceMapper.toDomain(null)).isNull();
    }

}
