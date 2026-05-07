package br.com.accenture.customer.domain.repository;

import br.com.accenture.customer.domain.model.Address;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository {

    Address save(Address address);

    Optional<Address> findById(UUID id);

    PageResult<Address> findByCustomerId(UUID customerId, PageRequest pageRequest);

    List<Address> findAll();

    void deleteById(UUID id);

}
