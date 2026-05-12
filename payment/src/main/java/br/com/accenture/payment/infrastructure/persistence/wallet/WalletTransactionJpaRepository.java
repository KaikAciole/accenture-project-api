package br.com.accenture.payment.infrastructure.persistence.wallet;

import br.com.accenture.payment.infrastructure.persistence.wallet.entity.WalletTransactionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletTransactionJpaRepository extends JpaRepository<WalletTransactionJpaEntity, UUID> {

    Page<WalletTransactionJpaEntity> findByWalletId(UUID walletId, Pageable pageable);
}