package br.com.accenture.customer.infrastructure.persistence;

import br.com.accenture.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, UUID> {

    Optional<CustomerJpaEntity> findByCpf(String cpf);

    Optional<CustomerJpaEntity> findByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<CustomerJpaEntity> findByNameContainingIgnoreCase(String name);

}
