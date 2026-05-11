package br.com.accenture.customer.domain.repository;

import br.com.accenture.customer.domain.model.Customer;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

}
