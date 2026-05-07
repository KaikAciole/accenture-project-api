package br.com.accenture.customer.domain.repository;

import br.com.accenture.customer.domain.model.Address;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository {

    Address save(Address address);

    Optional<Address> findById(UUID id);

    List<Address> findByCustomerId(UUID customerId);

    List<Address> findAll();

    void deleteById(UUID id);

}
