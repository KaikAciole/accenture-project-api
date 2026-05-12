package br.com.accenture.payment.infrastructure.persistence.wallet.mapper;

import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.infrastructure.persistence.wallet.entity.WalletJpaEntity;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WalletPersistenceMapperTest {

    @Test
    void toEntityMapsDomainWallet() {
        var entity = WalletPersistenceMapper.toEntity(TestFixtures.walletWithBalance());

        assertThat(entity.getId()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(entity.getOwnerId()).isEqualTo(TestFixtures.OWNER_ID);
        assertThat(entity.getOwnerType()).isEqualTo(WalletOwnerType.COSTUMER);
        assertThat(entity.getBalance()).isEqualByComparingTo(TestFixtures.WALLET_BALANCE);
        assertThat(entity.getCreatedAt()).isEqualTo(TestFixtures.CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(TestFixtures.UPDATED_AT);
        assertThat(entity.getVersion()).isEqualTo(1L);
    }

    @Test
    void toDomainMapsEntityWallet() {
        var entity = WalletJpaEntity.builder()
                .id(TestFixtures.WALLET_ID)
                .ownerId(TestFixtures.OWNER_ID)
                .ownerType(WalletOwnerType.COSTUMER)
                .balance(TestFixtures.WALLET_BALANCE)
                .createdAt(TestFixtures.CREATED_AT)
                .updatedAt(TestFixtures.UPDATED_AT)
                .version(1L)
                .build();

        var wallet = WalletPersistenceMapper.toDomain(entity);

        assertThat(wallet.getId()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(wallet.getOwnerId()).isEqualTo(TestFixtures.OWNER_ID);
        assertThat(wallet.getOwnerType()).isEqualTo(WalletOwnerType.COSTUMER);
        assertThat(wallet.getBalance()).isEqualByComparingTo(TestFixtures.WALLET_BALANCE);
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(WalletPersistenceMapper.toEntity(null)).isNull();
        assertThat(WalletPersistenceMapper.toDomain(null)).isNull();
    }
}
