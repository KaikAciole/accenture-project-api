package br.com.accenture.payment.infrastructure.persistence.wallet;

import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.model.Wallet;
import br.com.accenture.payment.infrastructure.config.JpaConfig;
import br.com.accenture.payment.support.TestFixtures;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({WalletRepositoryAdapter.class, JpaConfig.class})
class WalletRepositoryAdapterTest {

    @Autowired
    private WalletRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    @Test
    void saveGeneratesIdAuditFieldsAndVersion() {
        Wallet saved = adapter.save(TestFixtures.newWallet());
        em.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOwnerId()).isEqualTo(TestFixtures.OWNER_ID);
        assertThat(saved.getOwnerType()).isEqualTo(WalletOwnerType.CUSTOMER);
        assertThat(saved.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
    }

    @Test
    void saveRejectsDuplicateOwnerWallet() {
        adapter.save(TestFixtures.newWallet());
        em.flush();
        em.clear();

        Wallet duplicate = Wallet.createNew(TestFixtures.OWNER_ID, WalletOwnerType.CUSTOMER);

        assertThatThrownBy(() -> {
            adapter.save(duplicate);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void findByIdFindByOwnerAndExistsReflectPersistedData() {
        Wallet saved = adapter.save(TestFixtures.newWallet());
        em.flush();
        em.clear();

        assertThat(adapter.findById(saved.getId())).isPresent();
        assertThat(adapter.findByOwnerIdAndOwnerType(TestFixtures.OWNER_ID, WalletOwnerType.CUSTOMER))
                .isPresent()
                .get()
                .extracting(Wallet::getId)
                .isEqualTo(saved.getId());
        assertThat(adapter.existsByOwnerIdAndOwnerType(TestFixtures.OWNER_ID, WalletOwnerType.CUSTOMER)).isTrue();
        assertThat(adapter.existsByOwnerIdAndOwnerType(UUID.randomUUID(), WalletOwnerType.CUSTOMER)).isFalse();
    }

    @Test
    void saveUpdatesBalancePreservingOwnerData() {
        Wallet saved = adapter.save(TestFixtures.newWallet());
        em.flush();
        em.clear();

        Wallet credited = Wallet.restore(
                saved.getId(),
                UUID.randomUUID(),
                WalletOwnerType.SELLER,
                new BigDecimal("75.00"),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                saved.getVersion()
        );

        adapter.save(credited);
        em.flush();
        em.clear();

        Wallet reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getOwnerId()).isEqualTo(TestFixtures.OWNER_ID);
        assertThat(reloaded.getOwnerType()).isEqualTo(WalletOwnerType.CUSTOMER);
        assertThat(reloaded.getBalance()).isEqualByComparingTo("75.00");
    }
}
