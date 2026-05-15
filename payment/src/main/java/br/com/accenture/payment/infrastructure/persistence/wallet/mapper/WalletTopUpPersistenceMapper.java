package br.com.accenture.payment.infrastructure.persistence.wallet.mapper;

import br.com.accenture.payment.domain.wallet.model.WalletTopUp;
import br.com.accenture.payment.infrastructure.persistence.wallet.entity.WalletTopUpJpaEntity;

public class WalletTopUpPersistenceMapper {

    private WalletTopUpPersistenceMapper() {
    }

    public static WalletTopUpJpaEntity toEntity(WalletTopUp walletTopUp) {
        return WalletTopUpJpaEntity.builder()
                .id(walletTopUp.getId())
                .walletId(walletTopUp.getWalletId())
                .customerId(walletTopUp.getCustomerId())
                .amount(walletTopUp.getAmount())
                .status(walletTopUp.getStatus())
                .externalOrderId(walletTopUp.getExternalOrderId())
                .clientToken(walletTopUp.getClientToken())
                .createdAt(walletTopUp.getCreatedAt())
                .updatedAt(walletTopUp.getUpdatedAt())
                .creditedAt(walletTopUp.getCreditedAt())
                .build();
    }

    public static WalletTopUp toDomain(WalletTopUpJpaEntity entity) {
        return WalletTopUp.restore(
                entity.getId(),
                entity.getWalletId(),
                entity.getCustomerId(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getExternalOrderId(),
                entity.getClientToken(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreditedAt()
        );
    }
}