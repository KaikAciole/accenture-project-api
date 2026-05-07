package br.com.accenture.customer.infrastructure.persistence;

import br.com.accenture.customer.infrastructure.persistence.entity.AddressJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AddressJpaRepository extends JpaRepository<AddressJpaEntity, UUID> {

    Page<AddressJpaEntity> findByCustomerId(UUID customerId, Pageable pageable);

}
