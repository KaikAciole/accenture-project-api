package br.com.accenture.customer.infrastructure.persistence;

import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.domain.pagination.Direction;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;
import br.com.accenture.customer.domain.pagination.Sort;
import br.com.accenture.customer.infrastructure.config.JpaConfig;
import br.com.accenture.customer.infrastructure.persistence.entity.AddressJpaEntity;
import br.com.accenture.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
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
        baseCustomer = Customer.createNew(
                "Maria", "maria@example.com", "12345678901", "secret123", "11999998888"
        );
    }

    @Test
    void save_shouldGenerateIdAndPopulateAuditFields() {
        Customer saved = adapter.save(baseCustomer);
        em.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCpf()).isEqualTo("12345678901");
    }

    @Test
    void save_shouldRejectDuplicateCpf() {
        adapter.save(baseCustomer);
        em.flush();
        em.clear();

        Customer duplicateCpf = Customer.createNew(
                "Joana", "joana@example.com", "12345678901", "secret123", "11900000001"
        );

        assertThatThrownBy(() -> {
            adapter.save(duplicateCpf);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void save_shouldRejectDuplicateEmail() {
        adapter.save(baseCustomer);
        em.flush();
        em.clear();

        Customer duplicateEmail = Customer.createNew(
                "Joana", "maria@example.com", "99988877766", "secret123", "11900000001"
        );

        assertThatThrownBy(() -> {
            adapter.save(duplicateEmail);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void save_shouldRejectDuplicatePhone() {
        adapter.save(baseCustomer);
        em.flush();
        em.clear();

        Customer duplicatePhone = Customer.createNew(
                "Joana", "joana@example.com", "99988877766", "secret123", "11999998888"
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
        assertThat(result.get().getEmail()).isEqualTo("maria@example.com");
    }

    @Test
    void findByCpf_shouldFindExisting() {
        adapter.save(baseCustomer);
        em.flush();

        assertThat(adapter.findByCpf("12345678901")).isPresent();
        assertThat(adapter.findByCpf("00000000000")).isEmpty();
    }

    @Test
    void findByEmail_shouldFindExisting() {
        adapter.save(baseCustomer);
        em.flush();

        assertThat(adapter.findByEmail("maria@example.com")).isPresent();
        assertThat(adapter.findByEmail("nope@example.com")).isEmpty();
    }

    @Test
    void existsByCpf_shouldReflectPresence() {
        adapter.save(baseCustomer);
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
        adapter.save(baseCustomer);
        em.flush();

        assertThat(adapter.existsByPhone("11999998888")).isTrue();
        assertThat(adapter.existsByPhone("00000000000")).isFalse();
    }

    @Test
    void findByNameContainingIgnoreCase_shouldFindCaseInsensitiveAndPaginate() {
        persistDirect("Maria Silva", "maria.silva@example.com", "11111111111", "11900000001");
        persistDirect("Marina Souza", "marina@example.com", "22222222222", "11900000002");
        persistDirect("João Pereira", "joao@example.com", "33333333333", "11900000003");

        PageResult<Customer> page = adapter.findByNameContainingIgnoreCase("MAR", PageRequest.of(0, 10));

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(Customer::getName)
                .containsExactlyInAnyOrder("Maria Silva", "Marina Souza");
    }

    @Test
    void findByNameContainingIgnoreCase_shouldReturnEmptyWhenNoMatch() {
        persistDirect("Maria Silva", "maria.silva@example.com", "11111111111", "11900000001");

        PageResult<Customer> page = adapter.findByNameContainingIgnoreCase("xyz", PageRequest.of(0, 10));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void findAll_shouldPaginateRespectingPageSize() {
        persistDirect("A", "a@example.com", "10000000001", "11900000001");
        persistDirect("B", "b@example.com", "10000000002", "11900000002");
        persistDirect("C", "c@example.com", "10000000003", "11900000003");

        PageResult<Customer> firstPage = adapter.findAll(PageRequest.of(0, 2));
        PageResult<Customer> secondPage = adapter.findAll(PageRequest.of(1, 2));

        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.content()).hasSize(2);
        assertThat(secondPage.content()).hasSize(1);
    }

    @Test
    void findAll_shouldRespectSortDescending() {
        persistDirect("A", "a@example.com", "10000000001", "11900000001");
        persistDirect("B", "b@example.com", "10000000002", "11900000002");
        persistDirect("C", "c@example.com", "10000000003", "11900000003");

        PageResult<Customer> page = adapter.findAll(
                PageRequest.of(0, 10, List.of(new Sort("name", Direction.DESC)))
        );

        assertThat(page.content()).extracting(Customer::getName).containsExactly("C", "B", "A");
    }

    @Test
    void findAll_shouldRespectSortAscending() {
        persistDirect("Carlos", "c@example.com", "10000000003", "11900000003");
        persistDirect("Ana", "a@example.com", "10000000001", "11900000001");
        persistDirect("Bruno", "b@example.com", "10000000002", "11900000002");

        PageResult<Customer> page = adapter.findAll(
                PageRequest.of(0, 10, List.of(new Sort("name", Direction.ASC)))
        );

        assertThat(page.content()).extracting(Customer::getName).containsExactly("Ana", "Bruno", "Carlos");
    }

    @Test
    void deleteById_shouldRemoveExisting() {
        Customer saved = adapter.save(baseCustomer);
        em.flush();

        adapter.deleteById(saved.getId());
        em.flush();
        em.clear();

        assertThat(adapter.findById(saved.getId())).isEmpty();
    }

    @Test
    void deleteById_shouldCascadeAssociatedAddresses() {
        Customer saved = adapter.save(baseCustomer);
        em.flush();

        CustomerJpaEntity customerRef = em.find(CustomerJpaEntity.class, saved.getId());
        AddressJpaEntity firstAddress = AddressJpaEntity.builder()
                .customer(customerRef)
                .street("Rua A")
                .number("100")
                .neighborhood("Centro")
                .city("São Paulo")
                .state("SP")
                .zipCode("01001000")
                .build();
        AddressJpaEntity secondAddress = AddressJpaEntity.builder()
                .customer(customerRef)
                .street("Rua B")
                .number("200")
                .neighborhood("Centro")
                .city("São Paulo")
                .state("SP")
                .zipCode("01001001")
                .build();
        em.persist(firstAddress);
        em.persist(secondAddress);
        em.flush();
        UUID firstAddressId = firstAddress.getId();
        UUID secondAddressId = secondAddress.getId();
        em.clear();

        adapter.deleteById(saved.getId());
        em.flush();
        em.clear();

        assertThat(adapter.findById(saved.getId())).isEmpty();
        assertThat(em.find(AddressJpaEntity.class, firstAddressId)).isNull();
        assertThat(em.find(AddressJpaEntity.class, secondAddressId)).isNull();
    }

    @Test
    void cpfShouldBeImmutableAtTheJpaLayer() {
        Customer saved = adapter.save(baseCustomer);
        em.flush();
        em.clear();

        Customer toUpdate = Customer.restore(
                saved.getId(), saved.getName(), saved.getEmail(), "99988877766",
                saved.getPassword(), saved.getPhone(), saved.getCreatedAt(), saved.getUpdatedAt()
        );
        adapter.save(toUpdate);
        em.flush();
        em.clear();

        Customer reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCpf()).isEqualTo("12345678901");
    }

    private void persistDirect(String name, String email, String cpf, String phone) {
        em.persist(CustomerJpaEntity.builder()
                .name(name)
                .email(email)
                .cpf(cpf)
                .password("secret123")
                .phone(phone)
                .build());
    }

}
