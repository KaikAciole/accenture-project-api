package br.com.accenture.customer.infrastructure.persistence;

import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;
import br.com.accenture.customer.infrastructure.config.JpaConfig;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({CustomerRepositoryAdapter.class, JpaConfig.class})
class CustomerRepositoryAdapterTest {

    @Autowired
    private CustomerRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    private Customer baseCustomer;

    @BeforeEach
    void setUp() {
        baseCustomer = aCustomer(1);
    }

    @Test
    void save_shouldGenerateIdAndPopulateAuditFields() {
        Customer saved = adapter.save(baseCustomer);
        em.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo(baseCustomer.getEmail());
    }

    @Test
    void save_shouldRejectDuplicateEmail() {
        adapter.save(baseCustomer);
        em.flush();
        em.clear();

        Customer duplicateEmail = Customer.create(
                "Other", baseCustomer.getEmail(), "20000000000", "21900000000"
        );

        assertThatThrownBy(() -> {
            adapter.save(duplicateEmail);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void save_shouldRejectDuplicateCpf() {
        adapter.save(baseCustomer);
        em.flush();
        em.clear();

        Customer duplicateCpf = Customer.create(
                "Other", "other@example.com", baseCustomer.getCpf(), "21900000001"
        );

        assertThatThrownBy(() -> {
            adapter.save(duplicateCpf);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void save_shouldRejectDuplicatePhone() {
        adapter.save(baseCustomer);
        em.flush();
        em.clear();

        Customer duplicatePhone = Customer.create(
                "Other", "other@example.com", "20000000002", baseCustomer.getPhone()
        );

        assertThatThrownBy(() -> {
            adapter.save(duplicatePhone);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void findById_shouldReturnEmptyWhenMissing() {
        Optional<Customer> result = adapter.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    void findById_shouldReturnPresentAfterSave() {
        Customer saved = adapter.save(baseCustomer);
        em.flush();
        em.clear();

        Optional<Customer> result = adapter.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(baseCustomer.getEmail());
    }

    @Test
    void existsByCpf_shouldReflectPresence() {
        adapter.save(baseCustomer);
        em.flush();

        assertThat(adapter.existsByCpf(baseCustomer.getCpf())).isTrue();
        assertThat(adapter.existsByCpf("00000000000")).isFalse();
    }

    @Test
    void existsByEmail_shouldReflectPresence() {
        adapter.save(baseCustomer);
        em.flush();

        assertThat(adapter.existsByEmail(baseCustomer.getEmail())).isTrue();
        assertThat(adapter.existsByEmail("nope@example.com")).isFalse();
    }

    @Test
    void existsByPhone_shouldReflectPresence() {
        adapter.save(baseCustomer);
        em.flush();

        assertThat(adapter.existsByPhone(baseCustomer.getPhone())).isTrue();
        assertThat(adapter.existsByPhone("00000000000")).isFalse();
    }

    @Test
    void deleteById_shouldRemoveCustomer() {
        Customer saved = adapter.save(baseCustomer);
        em.flush();
        em.clear();

        adapter.deleteById(saved.getId());
        em.flush();

        assertThat(adapter.findById(saved.getId())).isEmpty();
        assertThat(adapter.existsByEmail(baseCustomer.getEmail())).isFalse();
    }

    @Test
    void deleteById_shouldAllowSubsequentReinsertionOfSameEmail() {
        Customer saved = adapter.save(baseCustomer);
        em.flush();

        adapter.deleteById(saved.getId());
        em.flush();
        em.clear();

        Customer reinserted = adapter.save(aCustomer(1));
        em.flush();

        assertThat(reinserted.getId()).isNotNull();
        assertThat(reinserted.getId()).isNotEqualTo(saved.getId());
    }

    @Test
    void findAll_shouldReturnAllCustomersPaginated() {
        adapter.save(aCustomer(10));
        adapter.save(aCustomer(11));
        adapter.save(aCustomer(12));
        em.flush();

        PageResult<Customer> page = adapter.findAll(PageRequest.of(0, 10));

        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.content()).extracting(Customer::getEmail)
                .containsExactlyInAnyOrder(
                        "customer10@example.com",
                        "customer11@example.com",
                        "customer12@example.com"
                );
    }

    @Test
    void findAll_shouldRespectPaginationSize() {
        adapter.save(aCustomer(20));
        adapter.save(aCustomer(21));
        adapter.save(aCustomer(22));
        em.flush();

        PageResult<Customer> firstPage = adapter.findAll(PageRequest.of(0, 2));
        PageResult<Customer> secondPage = adapter.findAll(PageRequest.of(1, 2));

        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.content()).hasSize(2);
        assertThat(secondPage.content()).hasSize(1);
    }

    @Test
    void findAll_shouldReturnEmptyPageWhenNoCustomers() {
        PageResult<Customer> page = adapter.findAll(PageRequest.of(0, 10));

        assertThat(page.totalElements()).isZero();
        assertThat(page.content()).isEmpty();
    }

    private Customer aCustomer(int seed) {
        return Customer.create(
                "Customer " + seed,
                "customer" + seed + "@example.com",
                String.format("%011d", 10000000000L + seed),
                String.format("%011d", 11000000000L + seed)
        );
    }

}
