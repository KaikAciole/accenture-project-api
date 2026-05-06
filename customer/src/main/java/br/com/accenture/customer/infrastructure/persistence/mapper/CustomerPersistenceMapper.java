package br.com.accenture.customer.infrastructure.persistence.mapper;

import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.infrastructure.persistence.entity.CustomerJpaEntity;

public final class CustomerPersistenceMapper {

    private CustomerPersistenceMapper() {
    }

    public static CustomerJpaEntity toEntity(Customer customer) {
        if (customer == null) {
            return null;
        }
        return CustomerJpaEntity.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .cpf(customer.getCpf())
                .password(customer.getPassword())
                .phone(customer.getPhone())
                .build();
    }

    public static Customer toDomain(CustomerJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Customer.restore(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getCpf(),
                entity.getPassword(),
                entity.getPhone(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

}
