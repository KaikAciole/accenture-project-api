package br.com.accenture.payment.infrastructure.persistence.wallet;

import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.model.Wallet;
import br.com.accenture.payment.domain.wallet.repository.WalletRepository;
import br.com.accenture.payment.infrastructure.persistence.wallet.mapper.WalletPersistenceMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class WalletRepositoryAdapter implements WalletRepository {

    private final WalletJpaRepository walletJpaRepository;

    public WalletRepositoryAdapter(WalletJpaRepository walletJpaRepository) {
        this.walletJpaRepository = walletJpaRepository;
    }

    @Override
    public Wallet save(Wallet wallet) {
        return WalletPersistenceMapper.toDomain(
                walletJpaRepository.save(WalletPersistenceMapper.toEntity(wallet))
        );
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return walletJpaRepository.findById(id)
                .map(WalletPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByOwnerIdAndOwnerType(UUID ownerId, WalletOwnerType ownerType) {
        return walletJpaRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
                .map(WalletPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByOwnerIdAndOwnerType(UUID ownerId, WalletOwnerType ownerType) {
        return walletJpaRepository.existsByOwnerIdAndOwnerType(ownerId, ownerType);
    }
}