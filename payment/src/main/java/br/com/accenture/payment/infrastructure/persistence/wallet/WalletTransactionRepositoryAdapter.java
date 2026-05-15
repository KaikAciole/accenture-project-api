package br.com.accenture.payment.infrastructure.persistence.wallet;

import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import br.com.accenture.payment.domain.wallet.model.WalletTransaction;
import br.com.accenture.payment.domain.wallet.repository.WalletTransactionRepository;
import br.com.accenture.payment.infrastructure.persistence.mapper.PageableMapper;
import br.com.accenture.payment.infrastructure.persistence.wallet.entity.WalletTransactionJpaEntity;
import br.com.accenture.payment.infrastructure.persistence.wallet.mapper.WalletTransactionPersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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
    public PageResult<WalletTransaction> findByWalletId(UUID walletId, PageRequest pageRequest) {
        Pageable pageable = PageableMapper.toPageable(pageRequest);

        Page<WalletTransactionJpaEntity> page = walletTransactionJpaRepository.findByWalletId(
                walletId,
                pageable
        );

        return new PageResult<>(
                page.getContent()
                        .stream()
                        .map(WalletTransactionPersistenceMapper::toDomain)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public boolean existsByPaymentIdAndReason(UUID paymentId, WalletTransactionReason reason) {
        return walletTransactionJpaRepository.existsByPaymentIdAndReason(paymentId, reason);
    }
}
