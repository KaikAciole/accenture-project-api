package br.com.accenture.payment.infrastructure.persistence.wallet;

import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.infrastructure.persistence.wallet.entity.WalletJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, UUID> {

    Optional<WalletJpaEntity> findByOwnerIdAndOwnerType(UUID ownerId, WalletOwnerType ownerType);

    boolean existsByOwnerIdAndOwnerType(UUID ownerId, WalletOwnerType ownerType);
}