package br.com.accenture.payment.infrastructure.persistence.payment;

import br.com.accenture.payment.domain.payment.enums.PaymentMethod;
import br.com.accenture.payment.domain.payment.enums.PaymentStatus;
import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.domain.pagination.Direction;
import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.Sort;
import br.com.accenture.payment.infrastructure.config.JpaConfig;
import br.com.accenture.payment.infrastructure.persistence.payment.entity.PaymentJpaEntity;
import br.com.accenture.payment.infrastructure.persistence.payment.PaymentRepositoryAdapter;
import br.com.accenture.payment.support.TestFixtures;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({PaymentRepositoryAdapter.class, JpaConfig.class})
class PaymentRepositoryAdapterTest {

    @Autowired
    private PaymentRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    @Test
    void saveGeneratesIdAndAuditFieldsForNewPayment() {
        Payment saved = adapter.save(TestFixtures.newPayment());
        em.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getOrderId()).isEqualTo(TestFixtures.ORDER_ID);
    }

    @Test
    void saveRejectsDuplicateOrderId() {
        adapter.save(TestFixtures.newPayment());
        em.flush();
        em.clear();

        Payment duplicate = Payment.createNew(
                TestFixtures.ORDER_ID,
                UUID.randomUUID(),
                new BigDecimal("20.00"),
                PaymentMethod.CREDIT_CARD
        );

        assertThatThrownBy(() -> {
            adapter.save(duplicate);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void findByIdFindByOrderIdAndExistsByOrderIdReflectPersistedData() {
        Payment saved = adapter.save(TestFixtures.newPayment());
        em.flush();
        em.clear();

        Optional<Payment> byId = adapter.findById(saved.getId());
        Optional<Payment> byOrderId = adapter.findByOrderId(TestFixtures.ORDER_ID);

        assertThat(byId).isPresent();
        assertThat(byId.get().getOrderId()).isEqualTo(TestFixtures.ORDER_ID);
        assertThat(byOrderId).isPresent();
        assertThat(byOrderId.get().getId()).isEqualTo(saved.getId());
        assertThat(adapter.existsByOrderId(TestFixtures.ORDER_ID)).isTrue();
        assertThat(adapter.existsByOrderId(UUID.randomUUID())).isFalse();
    }

    @Test
    void findMethodsReturnEmptyWhenMissing() {
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
        assertThat(adapter.findByOrderId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void saveUpdatesMutableFieldsPreservingImmutableJpaColumns() {
        Payment saved = adapter.save(TestFixtures.newPayment());
        em.flush();
        em.clear();

        Payment approved = Payment.restore(
                saved.getId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("999.00"),
                PaymentMethod.CREDIT_CARD,
                PaymentStatus.APPROVED,
                "tx-approved",
                null,
                TestFixtures.PAID_AT,
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                saved.getVersion()
        );

        adapter.save(approved);
        em.flush();
        em.clear();

        Payment reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getOrderId()).isEqualTo(TestFixtures.ORDER_ID);
        assertThat(reloaded.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(reloaded.getExternalTransactionId()).isEqualTo("tx-approved");
        assertThat(reloaded.getPaidAt()).isEqualTo(TestFixtures.PAID_AT);
    }

    @Test
    void findAllPaginatesAndSorts() {
        persistDirect("10000000-0000-0000-0000-000000000001", "10.00", PaymentStatus.PENDING);
        persistDirect("10000000-0000-0000-0000-000000000002", "30.00", PaymentStatus.APPROVED);
        persistDirect("10000000-0000-0000-0000-000000000003", "20.00", PaymentStatus.PROCESSING);
        em.flush();
        em.clear();

        var page = adapter.findAll(PageRequest.of(0, 2, List.of(new Sort("amount", Direction.DESC))));
        var secondPage = adapter.findAll(PageRequest.of(1, 2, List.of(new Sort("amount", Direction.DESC))));

        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.content()).extracting(Payment::getAmount)
                .containsExactly(new BigDecimal("30.00"), new BigDecimal("20.00"));
        assertThat(secondPage.content()).hasSize(1);
    }

    @Test
    void deleteByIdRemovesPayment() {
        Payment saved = adapter.save(TestFixtures.newPayment());
        em.flush();
        em.clear();

        adapter.deleteById(saved.getId());
        em.flush();
        em.clear();

        assertThat(adapter.findById(saved.getId())).isEmpty();
    }

    private void persistDirect(String orderId, String amount, PaymentStatus status) {
        em.persist(PaymentJpaEntity.builder()
                .orderId(UUID.fromString(orderId))
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal(amount))
                .method(PaymentMethod.PIX)
                .status(status)
                .build());
    }
}
