package br.com.accenture.order.infrastructure.persistence;

import br.com.accenture.order.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {
    Page<OrderJpaEntity> findByCustomerId(UUID customerId, Pageable pageable);
}