package br.com.accenture.payment.infrastructure.persistence.wallet.mapper;

import br.com.accenture.payment.domain.wallet.model.WalletTransaction;
import br.com.accenture.payment.infrastructure.persistence.wallet.entity.WalletTransactionJpaEntity;

public class WalletTransactionPersistenceMapper {

    private WalletTransactionPersistenceMapper() {
    }

    public static WalletTransactionJpaEntity toEntity(WalletTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        return WalletTransactionJpaEntity.builder()
                .id(transaction.getId())
                .walletId(transaction.getWalletId())
                .paymentId(transaction.getPaymentId())
                .type(transaction.getType())
                .reason(transaction.getReason())
                .amount(transaction.getAmount())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    public static WalletTransaction toDomain(WalletTransactionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return WalletTransaction.restore(
                entity.getId(),
                entity.getWalletId(),
                entity.getPaymentId(),
                entity.getType(),
                entity.getReason(),
                entity.getAmount(),
                entity.getCreatedAt()
        );
    }
}