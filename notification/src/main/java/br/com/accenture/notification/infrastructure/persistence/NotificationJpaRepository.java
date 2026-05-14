package br.com.accenture.notification.infrastructure.persistence;

import br.com.accenture.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    Optional<NotificationJpaEntity> findFirstByCustomerIdOrderByCreatedAtAsc(String customerId);
}
