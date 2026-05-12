package br.com.accenture.payment.infrastructure.persistence.wallet;

import br.com.accenture.payment.domain.pagination.Direction;
import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.Sort;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionType;
import br.com.accenture.payment.domain.wallet.model.WalletTransaction;
import br.com.accenture.payment.infrastructure.config.JpaConfig;
import br.com.accenture.payment.infrastructure.persistence.wallet.entity.WalletTransactionJpaEntity;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({WalletTransactionRepositoryAdapter.class, JpaConfig.class})
class WalletTransactionRepositoryAdapterTest {

    @Autowired
    private WalletTransactionRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    @Test
    void saveGeneratesIdAndCreatedAt() {
        WalletTransaction saved = adapter.save(WalletTransaction.credit(
                TestFixtures.WALLET_ID,
                TestFixtures.PAYMENT_ID,
                new BigDecimal("30.00"),
                WalletTransactionReason.TOP_UP
        ));
        em.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getWalletId()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(saved.getPaymentId()).isEqualTo(TestFixtures.PAYMENT_ID);
        assertThat(saved.getType()).isEqualTo(WalletTransactionType.CREDIT);
        assertThat(saved.getReason()).isEqualTo(WalletTransactionReason.TOP_UP);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByWalletIdPaginatesAndSortsOnlyWalletTransactions() {
        persistDirect(TestFixtures.WALLET_ID, "10.00", WalletTransactionType.CREDIT, WalletTransactionReason.TOP_UP);
        persistDirect(TestFixtures.WALLET_ID, "30.00", WalletTransactionType.DEBIT, WalletTransactionReason.PAYMENT);
        persistDirect(TestFixtures.WALLET_ID, "20.00", WalletTransactionType.CREDIT, WalletTransactionReason.REFUND);
        persistDirect(UUID.randomUUID(), "99.00", WalletTransactionType.CREDIT, WalletTransactionReason.TOP_UP);
        em.flush();
        em.clear();

        var page = adapter.findByWalletId(
                TestFixtures.WALLET_ID,
                PageRequest.of(0, 2, List.of(new Sort("amount", Direction.DESC)))
        );
        var secondPage = adapter.findByWalletId(
                TestFixtures.WALLET_ID,
                PageRequest.of(1, 2, List.of(new Sort("amount", Direction.DESC)))
        );

        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.content()).extracting(WalletTransaction::getAmount)
                .containsExactly(new BigDecimal("30.00"), new BigDecimal("20.00"));
        assertThat(secondPage.content()).hasSize(1);
    }

    private void persistDirect(UUID walletId,
                               String amount,
                               WalletTransactionType type,
                               WalletTransactionReason reason) {
        em.persist(WalletTransactionJpaEntity.builder()
                .walletId(walletId)
                .paymentId(UUID.randomUUID())
                .type(type)
                .reason(reason)
                .amount(new BigDecimal(amount))
                .build());
    }
}
