package br.com.accenture.payment.infrastructure.persistence.wallet.mapper;

import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionType;
import br.com.accenture.payment.infrastructure.persistence.wallet.entity.WalletTransactionJpaEntity;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WalletTransactionPersistenceMapperTest {

    @Test
    void toEntityMapsDomainTransaction() {
        var entity = WalletTransactionPersistenceMapper.toEntity(TestFixtures.walletCreditTransaction());

        assertThat(entity.getId()).isEqualTo(TestFixtures.WALLET_TRANSACTION_ID);
        assertThat(entity.getWalletId()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(entity.getPaymentId()).isEqualTo(TestFixtures.PAYMENT_ID);
        assertThat(entity.getType()).isEqualTo(WalletTransactionType.CREDIT);
        assertThat(entity.getReason()).isEqualTo(WalletTransactionReason.TOP_UP);
        assertThat(entity.getAmount()).isEqualByComparingTo(TestFixtures.AMOUNT);
        assertThat(entity.getCreatedAt()).isEqualTo(TestFixtures.CREATED_AT);
    }

    @Test
    void toDomainMapsEntityTransaction() {
        var entity = WalletTransactionJpaEntity.builder()
                .id(TestFixtures.WALLET_TRANSACTION_ID)
                .walletId(TestFixtures.WALLET_ID)
                .paymentId(TestFixtures.PAYMENT_ID)
                .type(WalletTransactionType.DEBIT)
                .reason(WalletTransactionReason.PAYMENT)
                .amount(TestFixtures.AMOUNT)
                .createdAt(TestFixtures.CREATED_AT)
                .build();

        var transaction = WalletTransactionPersistenceMapper.toDomain(entity);

        assertThat(transaction.getId()).isEqualTo(TestFixtures.WALLET_TRANSACTION_ID);
        assertThat(transaction.getWalletId()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(transaction.getPaymentId()).isEqualTo(TestFixtures.PAYMENT_ID);
        assertThat(transaction.getType()).isEqualTo(WalletTransactionType.DEBIT);
        assertThat(transaction.getReason()).isEqualTo(WalletTransactionReason.PAYMENT);
        assertThat(transaction.getAmount()).isEqualByComparingTo(TestFixtures.AMOUNT);
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(WalletTransactionPersistenceMapper.toEntity(null)).isNull();
        assertThat(WalletTransactionPersistenceMapper.toDomain(null)).isNull();
    }
}
