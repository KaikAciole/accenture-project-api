package br.com.accenture.customer.application.service;

import br.com.accenture.customer.domain.exception.CustomerNotFoundException;
import br.com.accenture.customer.domain.exception.DuplicateCustomerException;
import br.com.accenture.customer.domain.exception.ImmutableFieldException;
import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;
import br.com.accenture.customer.domain.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer newCustomer;
    private Customer existing;
    private UUID existingId;

    @BeforeEach
    void setUp() {
        newCustomer = Customer.createMinimal("maria@example.com");
        existingId = UUID.randomUUID();
        existing = Customer.restore(
                existingId, "Maria", "maria@example.com", "12345678901", "11999998888",
                Instant.now(), Instant.now()
        );
    }

    @Test
    void findById_shouldReturnCustomerWhenExists() {
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));

        Customer found = customerService.findById(existingId);

        assertThat(found.getId()).isEqualTo(existingId);
        assertThat(found.getEmail()).isEqualTo("maria@example.com");
    }

    @Test
    void findById_shouldThrowWhenCustomerNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(id))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void findAll_shouldDelegateToRepository() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageResult<Customer> page = new PageResult<>(List.of(existing), 0, 10, 1, 1);
        when(customerRepository.findAll(pageRequest)).thenReturn(page);

        PageResult<Customer> result = customerService.findAll(pageRequest);

        assertThat(result.content()).containsExactly(existing);
        assertThat(result.totalElements()).isEqualTo(1);
        verify(customerRepository).findAll(pageRequest);
    }

    @Test
    void findAll_shouldReturnEmptyPageWhenNoCustomers() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageResult<Customer> empty = new PageResult<>(List.of(), 0, 10, 0, 0);
        when(customerRepository.findAll(pageRequest)).thenReturn(empty);

        PageResult<Customer> result = customerService.findAll(pageRequest);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void create_shouldPersistWhenEmailIsUnique() {
        when(customerRepository.existsByEmail(newCustomer.getEmail())).thenReturn(false);
        when(customerRepository.save(newCustomer)).thenReturn(existing);

        Customer saved = customerService.create(newCustomer);

        assertThat(saved.getId()).isEqualTo(existingId);
        verify(customerRepository).save(newCustomer);
    }

    @Test
    void create_shouldFailWhenEmailAlreadyExists() {
        when(customerRepository.existsByEmail(newCustomer.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(newCustomer))
                .isInstanceOf(DuplicateCustomerException.class)
                .hasMessageContaining("email");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void update_shouldPersistMutableFields() {
        Customer payload = Customer.restore(
                null, "Maria Updated", null, null, "11988887777", null, null
        );
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByPhone("11988887777")).thenReturn(false);
        when(customerRepository.save(existing)).thenReturn(existing);

        Customer updated = customerService.update(existingId, payload);

        assertThat(updated.getName()).isEqualTo("Maria Updated");
        assertThat(updated.getPhone()).isEqualTo("11988887777");
        assertThat(updated.getCpf()).isEqualTo("12345678901");
        verify(customerRepository).save(existing);
    }

    @Test
    void update_shouldThrowWhenCustomerNotFound() {
        UUID id = UUID.randomUUID();
        Customer payload = Customer.restore(null, "Maria", null, null, null, null, null);
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update(id, payload))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void update_shouldRejectChangingCpfWhenAlreadyDefined() {
        Customer payload = Customer.restore(null, null, null, "99999999999", null, null, null);
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> customerService.update(existingId, payload))
                .isInstanceOf(ImmutableFieldException.class)
                .hasMessageContaining("cpf");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void update_shouldAllowFirstCpfDefinition() {
        Customer minimal = Customer.restore(
                existingId, null, "maria@example.com", null, null, Instant.now(), Instant.now()
        );
        Customer payload = Customer.restore(null, null, null, "12345678901", null, null, null);

        when(customerRepository.findById(existingId)).thenReturn(Optional.of(minimal));
        when(customerRepository.existsByCpf("12345678901")).thenReturn(false);
        when(customerRepository.save(minimal)).thenReturn(minimal);

        customerService.update(existingId, payload);

        assertThat(minimal.getCpf()).isEqualTo("12345678901");
        verify(customerRepository).existsByCpf("12345678901");
    }

    @Test
    void update_shouldFailWhenCpfAlreadyTakenByAnotherCustomer() {
        Customer minimal = Customer.restore(
                existingId, null, "maria@example.com", null, null, Instant.now(), Instant.now()
        );
        Customer payload = Customer.restore(null, null, null, "12345678901", null, null, null);

        when(customerRepository.findById(existingId)).thenReturn(Optional.of(minimal));
        when(customerRepository.existsByCpf("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(existingId, payload))
                .isInstanceOf(DuplicateCustomerException.class)
                .hasMessageContaining("cpf");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void update_shouldFailWhenNewPhoneIsTakenByAnotherCustomer() {
        Customer payload = Customer.restore(null, null, null, null, "11900000000", null, null);
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByPhone("11900000000")).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(existingId, payload))
                .isInstanceOf(DuplicateCustomerException.class)
                .hasMessageContaining("phone");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void update_shouldNotCheckUniquenessWhenFieldsAreUnchanged() {
        Customer payload = Customer.restore(
                null, "Maria New", null, "12345678901", "11999998888", null, null
        );
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));
        when(customerRepository.save(existing)).thenReturn(existing);

        customerService.update(existingId, payload);

        verify(customerRepository, never()).existsByCpf(any());
        verify(customerRepository, never()).existsByPhone(any());
    }

    @Test
    void delete_shouldRemoveExistingCustomer() {
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));

        customerService.delete(existingId);

        verify(customerRepository).deleteById(existingId);
    }

    @Test
    void delete_shouldThrowWhenCustomerNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.delete(id))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(customerRepository, never()).deleteById(any());
    }

}
