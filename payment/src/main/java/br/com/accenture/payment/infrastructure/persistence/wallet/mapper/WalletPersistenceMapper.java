package br.com.accenture.payment.infrastructure.persistence.wallet.mapper;

import br.com.accenture.payment.domain.wallet.model.Wallet;
import br.com.accenture.payment.infrastructure.persistence.wallet.entity.WalletJpaEntity;

public class WalletPersistenceMapper {

    private WalletPersistenceMapper() {
    }

    public static WalletJpaEntity toEntity(Wallet wallet) {
        if (wallet == null) {
            return null;
        }

        return WalletJpaEntity.builder()
                .id(wallet.getId())
                .ownerId(wallet.getOwnerId())
                .ownerType(wallet.getOwnerType())
                .balance(wallet.getBalance())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .version(wallet.getVersion())
                .build();
    }

    public static Wallet toDomain(WalletJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Wallet.restore(
                entity.getId(),
                entity.getOwnerId(),
                entity.getOwnerType(),
                entity.getBalance(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}