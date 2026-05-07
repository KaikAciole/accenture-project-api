package br.com.accenture.inventory.infrastructure.persistence;

import br.com.accenture.inventory.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    Optional<ProductJpaEntity> findBySku(String sku);

    boolean existsBySku(String sku);

    List<ProductJpaEntity> findByNameContainingIgnoreCase(String name);
}