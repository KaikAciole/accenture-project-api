package br.com.accenture.payment.infrastructure.persistence.wallet;

import br.com.accenture.payment.domain.wallet.model.WalletTransaction;
import br.com.accenture.payment.domain.wallet.repository.WalletTransactionRepository;
import br.com.accenture.payment.infrastructure.persistence.wallet.mapper.WalletTransactionPersistenceMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class WalletTransactionRepositoryAdapter implements WalletTransactionRepository {

    private final WalletTransactionJpaRepository walletTransactionJpaRepository;

    public WalletTransactionRepositoryAdapter(WalletTransactionJpaRepository walletTransactionJpaRepository) {
        this.walletTransactionJpaRepository = walletTransactionJpaRepository;
    }

    @Override
    public WalletTransaction save(WalletTransaction transaction) {
        return WalletTransactionPersistenceMapper.toDomain(
                walletTransactionJpaRepository.save(
                        WalletTransactionPersistenceMapper.toEntity(transaction)
                )
        );
    }

    @Override
    public List<WalletTransaction> findByWalletId(UUID walletId) {
        return walletTransactionJpaRepository.findByWalletId(walletId)
                .stream()
                .map(WalletTransactionPersistenceMapper::toDomain)
                .toList();
    }
}