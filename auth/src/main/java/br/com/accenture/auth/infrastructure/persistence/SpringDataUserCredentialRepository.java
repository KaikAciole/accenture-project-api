package br.com.accenture.auth.infrastructure.persistence;

import br.com.accenture.auth.infrastructure.persistence.entity.UserCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserCredentialRepository extends JpaRepository<UserCredentialEntity, UUID> {
    Optional<UserCredentialEntity> findByEmail(String email);
    Optional<UserCredentialEntity> findByCustomerId(UUID customerId);
    boolean existsByEmail(String email);
}