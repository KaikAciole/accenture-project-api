package br.com.accenture.customer.api.mapper;

import br.com.accenture.customer.api.dto.CreateCustomerInternalRequest;
import br.com.accenture.customer.api.dto.CustomerResponse;
import br.com.accenture.customer.api.dto.UpdateProfileRequest;
import br.com.accenture.customer.domain.model.Customer;

public final class CustomerDtoMapper {

    private CustomerDtoMapper() {
    }

    public static Customer toDomain(CreateCustomerInternalRequest request) {
        if (request == null) {
            return null;
        }
        return Customer.createMinimal(request.email());
    }

    public static Customer toDomainForUpdate(UpdateProfileRequest request) {
        if (request == null) {
            return null;
        }
        return Customer.restore(
                null,
                request.name(),
                null,
                request.cpf(),
                request.phone(),
                null,
                null
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
