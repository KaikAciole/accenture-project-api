package br.com.accenture.customer.domain.repository;

import br.com.accenture.customer.domain.model.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository {

    Address save(Address address);

    Optional<Address> findById(UUID id);

    Page<Address> findByCustomerId(UUID customerId, Pageable pageable);

    List<Address> findAll();

    void deleteById(UUID id);

}
