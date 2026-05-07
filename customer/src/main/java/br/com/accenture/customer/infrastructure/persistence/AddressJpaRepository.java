package br.com.accenture.customer.infrastructure.persistence;

import br.com.accenture.customer.infrastructure.persistence.entity.AddressJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddressJpaRepository extends JpaRepository<AddressJpaEntity, UUID> {

    List<AddressJpaEntity> findByCustomerId(UUID customerId);

}
