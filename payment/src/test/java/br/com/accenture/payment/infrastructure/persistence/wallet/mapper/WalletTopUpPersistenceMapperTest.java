package br.com.accenture.payment.infrastructure.persistence.wallet.mapper;

import br.com.accenture.payment.domain.wallet.enums.WalletTopUpStatus;
import br.com.accenture.payment.domain.wallet.model.WalletTopUp;
import br.com.accenture.payment.infrastructure.persistence.wallet.entity.WalletTopUpJpaEntity;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalletTopUpPersistenceMapperTest {

    @Test
    void toEntityMapsAllFields() {
        UUID id = UUID.fromString("c0f1ad34-bbc9-4b40-9c1f-1ab9c66af1aa");
        Instant createdAt = TestFixtures.CREATED_AT;
        Instant updatedAt = TestFixtures.UPDATED_AT;
        Instant creditedAt = TestFixtures.PAID_AT;
        WalletTopUp topUp = WalletTopUp.restore(
                id,
                TestFixtures.WALLET_ID,
                TestFixtures.CUSTOMER_ID,
                new BigDecimal("80.00"),
                WalletTopUpStatus.APPROVED,
                "ext-1",
                "token-1",
                createdAt,
                updatedAt,
                creditedAt
        );

        WalletTopUpJpaEntity entity = WalletTopUpPersistenceMapper.toEntity(topUp);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getWalletId()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(entity.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(entity.getAmount()).isEqualByComparingTo("80.00");
        assertThat(entity.getStatus()).isEqualTo(WalletTopUpStatus.APPROVED);
        assertThat(entity.getExternalOrderId()).isEqualTo("ext-1");
        assertThat(entity.getClientToken()).isEqualTo("token-1");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(entity.getCreditedAt()).isEqualTo(creditedAt);
    }

    @Test
    void toDomainMapsAllFields() {
        UUID id = UUID.fromString("c0f1ad34-bbc9-4b40-9c1f-1ab9c66af1aa");
        Instant createdAt = TestFixtures.CREATED_AT;
        Instant updatedAt = TestFixtures.UPDATED_AT;
        Instant creditedAt = TestFixtures.PAID_AT;
        WalletTopUpJpaEntity entity = WalletTopUpJpaEntity.builder()
                .id(id)
                .walletId(TestFixtures.WALLET_ID)
                .customerId(TestFixtures.CUSTOMER_ID)
                .amount(new BigDecimal("80.00"))
                .status(WalletTopUpStatus.PENDING)
                .externalOrderId("ext-2")
                .clientToken("token-2")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .creditedAt(creditedAt)
                .build();

        WalletTopUp domain = WalletTopUpPersistenceMapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getWalletId()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(domain.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(domain.getAmount()).isEqualByComparingTo("80.00");
        assertThat(domain.getStatus()).isEqualTo(WalletTopUpStatus.PENDING);
        assertThat(domain.getExternalOrderId()).isEqualTo("ext-2");
        assertThat(domain.getClientToken()).isEqualTo("token-2");
        assertThat(domain.getCreatedAt()).isEqualTo(createdAt);
        assertThat(domain.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(domain.getCreditedAt()).isEqualTo(creditedAt);
    }
}
