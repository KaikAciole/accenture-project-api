package br.com.accenture.payment.infrastructure.persistence.wallet;

import br.com.accenture.payment.domain.wallet.model.WalletTopUp;
import br.com.accenture.payment.domain.wallet.repository.WalletTopUpRepository;
import br.com.accenture.payment.infrastructure.persistence.wallet.mapper.WalletTopUpPersistenceMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class WalletTopUpRepositoryAdapter implements WalletTopUpRepository {

    private final WalletTopUpJpaRepository jpaRepository;

    public WalletTopUpRepositoryAdapter(WalletTopUpJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public WalletTopUp save(WalletTopUp walletTopUp) {
        var entity = WalletTopUpPersistenceMapper.toEntity(walletTopUp);
        var savedEntity = jpaRepository.save(entity);

        return WalletTopUpPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<WalletTopUp> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(WalletTopUpPersistenceMapper::toDomain);
    }

    @Override
    public Optional<WalletTopUp> findByExternalOrderId(String externalOrderId) {
        return jpaRepository.findByExternalOrderId(externalOrderId)
                .map(WalletTopUpPersistenceMapper::toDomain);
    }
}