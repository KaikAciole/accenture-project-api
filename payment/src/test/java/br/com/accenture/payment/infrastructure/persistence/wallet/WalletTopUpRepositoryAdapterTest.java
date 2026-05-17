package br.com.accenture.payment.infrastructure.persistence.wallet;

import br.com.accenture.payment.domain.wallet.enums.WalletTopUpStatus;
import br.com.accenture.payment.domain.wallet.model.WalletTopUp;
import br.com.accenture.payment.infrastructure.config.JpaConfig;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({WalletTopUpRepositoryAdapter.class, JpaConfig.class})
class WalletTopUpRepositoryAdapterTest {

    @Autowired
    private WalletTopUpRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    @Test
    void saveRoundTripsAllFields() {
        WalletTopUp topUp = WalletTopUp.createNew(
                TestFixtures.WALLET_ID,
                TestFixtures.CUSTOMER_ID,
                new BigDecimal("80.00")
        );

        WalletTopUp saved = adapter.save(topUp);
        em.flush();

        assertThat(saved.getId()).isEqualTo(topUp.getId());
        assertThat(saved.getWalletId()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(saved.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(saved.getAmount()).isEqualByComparingTo("80.00");
        assertThat(saved.getStatus()).isEqualTo(WalletTopUpStatus.PENDING);
    }

    @Test
    void findByIdAndFindByExternalOrderIdReturnPersistedTopUp() {
        WalletTopUp topUp = WalletTopUp.createNew(
                TestFixtures.WALLET_ID,
                TestFixtures.CUSTOMER_ID,
                new BigDecimal("80.00")
        );
        topUp.attachExternalOrder("ext-99", "token-99");
        WalletTopUp saved = adapter.save(topUp);
        em.flush();
        em.clear();

        assertThat(adapter.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(WalletTopUp::getExternalOrderId)
                .isEqualTo("ext-99");
        assertThat(adapter.findByExternalOrderId("ext-99"))
                .isPresent()
                .get()
                .extracting(WalletTopUp::getId)
                .isEqualTo(saved.getId());
        assertThat(adapter.findByExternalOrderId("missing")).isEmpty();
    }
}
