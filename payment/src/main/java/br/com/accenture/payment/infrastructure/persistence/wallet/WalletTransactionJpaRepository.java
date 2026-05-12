package br.com.accenture.payment.infrastructure.persistence.wallet;

import br.com.accenture.payment.infrastructure.persistence.wallet.entity.WalletTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WalletTransactionJpaRepository extends JpaRepository<WalletTransactionJpaEntity, UUID> {

    List<WalletTransactionJpaEntity> findByWalletId(UUID walletId);
}