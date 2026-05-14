package br.com.accenture.auth.infrastructure.persistence.repository;

import br.com.accenture.auth.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
}