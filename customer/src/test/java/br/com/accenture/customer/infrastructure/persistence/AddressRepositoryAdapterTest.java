package br.com.accenture.customer.infrastructure.persistence;

import br.com.accenture.customer.domain.model.Address;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;
import br.com.accenture.customer.infrastructure.config.JpaConfig;
import br.com.accenture.customer.infrastructure.persistence.entity.AddressJpaEntity;
import br.com.accenture.customer.infrastructure.persistence.entity.CustomerJpaEntity;
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

@DataJpaTest
@Import({AddressRepositoryAdapter.class, JpaConfig.class})
class AddressRepositoryAdapterTest {

    @Autowired
    private AddressRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager em;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = persistCustomer("Maria", "maria@example.com", "12345678901", "11999998888");
    }

    @Test
    void save_shouldGenerateIdAndPopulateAuditFields() {
        Address address = Address.createNew(
                customerId, "Rua A", "100", "Apt 1", "Centro", "São Paulo", "SP", "01001000"
        );

        Address saved = adapter.save(address);
        em.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_shouldUseEntityManagerReferenceWithoutLoadingCustomer() {
        Address address = Address.createNew(
                customerId, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000"
        );

        Address saved = adapter.save(address);
        em.flush();
        em.clear();

        AddressJpaEntity reloaded = em.find(AddressJpaEntity.class, saved.getId());
        assertThat(reloaded.getCustomer().getId()).isEqualTo(customerId);
    }

    @Test
    void findById_shouldReturnEmptyWhenMissing() {
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findById_shouldReturnAddressWithExposedCustomerId() {
        Address saved = adapter.save(Address.createNew(
                customerId, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000"
        ));
        em.flush();
        em.clear();

        Optional<Address> reloaded = adapter.findById(saved.getId());

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getCustomerId()).isEqualTo(customerId);
        assertThat(reloaded.get().getStreet()).isEqualTo("Rua A");
    }

    @Test
    void findByCustomerId_shouldReturnOnlyAddressesOfThatCustomer() {
        UUID otherCustomer = persistCustomer("Joana", "joana@example.com", "99988877766", "11900000001");
        adapter.save(Address.createNew(customerId, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000"));
        adapter.save(Address.createNew(customerId, "Rua B", "200", null, "Centro", "São Paulo", "SP", "01001001"));
        adapter.save(Address.createNew(otherCustomer, "Rua C", "300", null, "Centro", "Rio", "RJ", "22222222"));
        em.flush();

        PageResult<Address> page = adapter.findByCustomerId(customerId, PageRequest.of(0, 10));

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(Address::getStreet)
                .containsExactlyInAnyOrder("Rua A", "Rua B");
    }

    @Test
    void findByCustomerId_shouldRespectPaginationSize() {
        adapter.save(Address.createNew(customerId, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000"));
        adapter.save(Address.createNew(customerId, "Rua B", "200", null, "Centro", "São Paulo", "SP", "01001001"));
        adapter.save(Address.createNew(customerId, "Rua C", "300", null, "Centro", "São Paulo", "SP", "01001002"));
        em.flush();

        PageResult<Address> firstPage = adapter.findByCustomerId(customerId, PageRequest.of(0, 2));
        PageResult<Address> secondPage = adapter.findByCustomerId(customerId, PageRequest.of(1, 2));

        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.content()).hasSize(2);
        assertThat(secondPage.content()).hasSize(1);
    }

    @Test
    void findByCustomerId_shouldReturnEmptyWhenCustomerHasNoAddresses() {
        PageResult<Address> page = adapter.findByCustomerId(UUID.randomUUID(), PageRequest.of(0, 10));

        assertThat(page.totalElements()).isZero();
        assertThat(page.content()).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllAddressesAcrossCustomers() {
        UUID otherCustomer = persistCustomer("Joana", "joana@example.com", "99988877766", "11900000001");
        adapter.save(Address.createNew(customerId, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000"));
        adapter.save(Address.createNew(otherCustomer, "Rua B", "200", null, "Centro", "Rio", "RJ", "22222222"));
        em.flush();

        List<Address> all = adapter.findAll();

        assertThat(all).hasSize(2);
    }

    @Test
    void deleteById_shouldRemoveExisting() {
        Address saved = adapter.save(Address.createNew(
                customerId, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000"
        ));
        em.flush();

        adapter.deleteById(saved.getId());
        em.flush();
        em.clear();

        assertThat(adapter.findById(saved.getId())).isEmpty();
    }

    private UUID persistCustomer(String name, String email, String cpf, String phone) {
        CustomerJpaEntity entity = CustomerJpaEntity.builder()
                .name(name)
                .email(email)
                .cpf(cpf)
                .password("secret123")
                .phone(phone)
                .build();
        em.persist(entity);
        em.flush();
        return entity.getId();
    }

}
