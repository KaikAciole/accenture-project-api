package br.com.accenture.customer.api.mapper;

import br.com.accenture.customer.api.dto.CustomerRequest;
import br.com.accenture.customer.api.dto.CustomerResponse;
import br.com.accenture.customer.domain.model.Customer;

public final class CustomerDtoMapper {

    private CustomerDtoMapper() {
    }

    public static Customer toDomain(CustomerRequest request) {
        if (request == null) {
            return null;
        }
        return Customer.createNew(
                request.name(),
                request.email(),
                request.cpf(),
                request.password(),
                request.phone()
        );
    }

    public static CustomerResponse toResponse(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getCpf(),
                customer.getPhone(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

}
