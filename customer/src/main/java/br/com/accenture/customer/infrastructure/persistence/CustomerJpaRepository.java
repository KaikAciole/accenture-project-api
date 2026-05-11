package br.com.accenture.customer.infrastructure.persistence;

import br.com.accenture.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, UUID> {

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

}
