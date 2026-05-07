package br.com.accenture.customer.domain.repository;

import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    Optional<Customer> findByCpf(String cpf);

    Optional<Customer> findByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    PageResult<Customer> findByNameContainingIgnoreCase(String name, PageRequest pageRequest);

    PageResult<Customer> findAll(PageRequest pageRequest);

    void deleteById(UUID id);

}
