package br.com.accenture.customer.repositories;

import br.com.accenture.customer.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByCpf(String cpf);
    Optional<Customer> findByEmail(String email);

    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    List<Customer> findByNameContainingIgnoreCase(String name);

}
