package br.com.accenture.payment.infrastructure.persistence.wallet;

import br.com.accenture.payment.infrastructure.persistence.wallet.entity.WalletTopUpJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletTopUpJpaRepository extends JpaRepository<WalletTopUpJpaEntity, UUID> {

    Optional<WalletTopUpJpaEntity> findByExternalOrderId(String externalOrderId);
}