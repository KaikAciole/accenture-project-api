package br.com.accenture.auth.infrastructure.persistence;

import br.com.accenture.auth.infrastructure.persistence.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    List<PasswordResetTokenEntity> findAllByCustomerIdAndUsedAtIsNullAndExpiresAtAfter(UUID customerId, Instant now);
}
