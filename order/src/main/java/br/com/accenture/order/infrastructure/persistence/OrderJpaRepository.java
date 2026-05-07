package br.com.accenture.order.infrastructure.persistence;

import br.com.accenture.order.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {
    List<OrderJpaEntity> findByCustomerId(String customerId);
}