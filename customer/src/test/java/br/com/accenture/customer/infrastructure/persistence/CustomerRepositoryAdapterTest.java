package br.com.accenture.customer.infrastructure.persistence;

import br.com.accenture.customer.domain.model.Customer;
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
        baseCustomer = Customer.createMinimal("maria@example.com");
    }

    @Test
    void save_shouldGenerateIdAndPopulateAuditFields() {
        Customer saved = adapter.save(baseCustomer);
        em.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("maria@example.com");
    }

    @Test
    void save_shouldRejectDuplicateEmail() {
        adapter.save(baseCustomer);
        em.flush();
        em.clear();

        Customer duplicateEmail = Customer.createMinimal("maria@example.com");

        assertThatThrownBy(() -> {
            adapter.save(duplicateEmail);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void save_shouldRejectDuplicateCpfWhenBothDefined() {
        Customer first = Customer.createMinimal("maria@example.com");
        first.updateProfile(null, "12345678901", null);
        adapter.save(first);
        em.flush();
        em.clear();

        Customer second = Customer.createMinimal("joana@example.com");
        second.updateProfile(null, "12345678901", null);

        assertThatThrownBy(() -> {
            adapter.save(second);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void save_shouldRejectDuplicatePhoneWhenBothDefined() {
        Customer first = Customer.createMinimal("maria@example.com");
        first.updateProfile(null, null, "11999998888");
        adapter.save(first);
        em.flush();
        em.clear();

        Customer second = Customer.createMinimal("joana@example.com");
        second.updateProfile(null, null, "11999998888");

        assertThatThrownBy(() -> {
            adapter.save(second);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void save_shouldAllowMultipleCustomersWithNullCpfAndPhone() {
        adapter.save(Customer.createMinimal("a@example.com"));
        em.flush();
        adapter.save(Customer.createMinimal("b@example.com"));
        em.flush();
        adapter.save(Customer.createMinimal("c@example.com"));
        em.flush();

        assertThat(adapter.existsByEmail("a@example.com")).isTrue();
        assertThat(adapter.existsByEmail("b@example.com")).isTrue();
        assertThat(adapter.existsByEmail("c@example.com")).isTrue();
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
        assertThat(result.get().getEmail()).isEqualTo("maria@example.com");
    }

    @Test
    void existsByCpf_shouldReflectPresence() {
        Customer customer = Customer.createMinimal("maria@example.com");
        customer.updateProfile(null, "12345678901", null);
        adapter.save(customer);
        em.flush();

        assertThat(adapter.existsByCpf("12345678901")).isTrue();
        assertThat(adapter.existsByCpf("00000000000")).isFalse();
    }

    @Test
    void existsByEmail_shouldReflectPresence() {
        adapter.save(baseCustomer);
        em.flush();

        assertThat(adapter.existsByEmail("maria@example.com")).isTrue();
        assertThat(adapter.existsByEmail("nope@example.com")).isFalse();
    }

    @Test
    void existsByPhone_shouldReflectPresence() {
        Customer customer = Customer.createMinimal("maria@example.com");
        customer.updateProfile(null, null, "11999998888");
        adapter.save(customer);
        em.flush();

        assertThat(adapter.existsByPhone("11999998888")).isTrue();
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
        assertThat(adapter.existsByEmail("maria@example.com")).isFalse();
    }

    @Test
    void deleteById_shouldAllowSubsequentReinsertionOfSameEmail() {
        Customer saved = adapter.save(baseCustomer);
        em.flush();

        adapter.deleteById(saved.getId());
        em.flush();
        em.clear();

        Customer reinserted = adapter.save(Customer.createMinimal("maria@example.com"));
        em.flush();

        assertThat(reinserted.getId()).isNotNull();
        assertThat(reinserted.getId()).isNotEqualTo(saved.getId());
    }

}
