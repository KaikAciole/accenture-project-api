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
import static org.mockito.Mockito.times;
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
        newCustomer = Customer.createNew("Maria", "maria@example.com", "12345678901", "secret123", "11999998888");
        existingId = UUID.randomUUID();
        existing = Customer.restore(
                existingId, "Maria", "maria@example.com", "12345678901", "secret123", "11999998888",
                Instant.now(), Instant.now()
        );
    }

    @Test
    void create_shouldPersistWhenAllFieldsAreUnique() {
        when(customerRepository.existsByCpf(newCustomer.getCpf())).thenReturn(false);
        when(customerRepository.existsByEmail(newCustomer.getEmail())).thenReturn(false);
        when(customerRepository.existsByPhone(newCustomer.getPhone())).thenReturn(false);
        when(customerRepository.save(newCustomer)).thenReturn(existing);

        Customer saved = customerService.create(newCustomer);

        assertThat(saved.getId()).isEqualTo(existingId);
        verify(customerRepository).save(newCustomer);
    }

    @Test
    void create_shouldFailWhenCpfAlreadyExists() {
        when(customerRepository.existsByCpf(newCustomer.getCpf())).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(newCustomer))
                .isInstanceOf(DuplicateCustomerException.class)
                .hasMessageContaining("cpf");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void create_shouldFailWhenEmailAlreadyExists() {
        when(customerRepository.existsByCpf(newCustomer.getCpf())).thenReturn(false);
        when(customerRepository.existsByEmail(newCustomer.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(newCustomer))
                .isInstanceOf(DuplicateCustomerException.class)
                .hasMessageContaining("email");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void create_shouldFailWhenPhoneAlreadyExists() {
        when(customerRepository.existsByCpf(newCustomer.getCpf())).thenReturn(false);
        when(customerRepository.existsByEmail(newCustomer.getEmail())).thenReturn(false);
        when(customerRepository.existsByPhone(newCustomer.getPhone())).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(newCustomer))
                .isInstanceOf(DuplicateCustomerException.class)
                .hasMessageContaining("phone");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void findAll_shouldDelegateToRepository() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageResult<Customer> page = new PageResult<>(List.of(existing), 0, 10, 1, 1);
        when(customerRepository.findAll(pageRequest)).thenReturn(page);

        PageResult<Customer> result = customerService.findAll(pageRequest);

        assertThat(result.content()).containsExactly(existing);
    }

    @Test
    void findByName_shouldDelegateToRepository() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageResult<Customer> page = new PageResult<>(List.of(existing), 0, 10, 1, 1);
        when(customerRepository.findByNameContainingIgnoreCase("ma", pageRequest)).thenReturn(page);

        PageResult<Customer> result = customerService.findByName("ma", pageRequest);

        assertThat(result.content()).containsExactly(existing);
    }

    @Test
    void findById_shouldReturnExistingCustomer() {
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));

        Customer result = customerService.findById(existingId);

        assertThat(result).isSameAs(existing);
    }

    @Test
    void findById_shouldThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(id))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void update_shouldPersistMutableFields() {
        Customer updatePayload = Customer.createNew(
                "Maria Updated", "new@example.com", "12345678901", "newpass12", "11988887777"
        );
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("11988887777")).thenReturn(false);
        when(customerRepository.save(existing)).thenReturn(existing);

        Customer updated = customerService.update(existingId, updatePayload);

        assertThat(updated.getName()).isEqualTo("Maria Updated");
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getPhone()).isEqualTo("11988887777");
        assertThat(updated.getCpf()).isEqualTo("12345678901");
        verify(customerRepository).save(existing);
    }

    @Test
    void update_shouldThrowWhenCustomerNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update(id, newCustomer))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void update_shouldRejectChangingCpf() {
        Customer payload = Customer.createNew(
                "Maria", "maria@example.com", "99999999999", "secret123", "11999998888"
        );
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> customerService.update(existingId, payload))
                .isInstanceOf(ImmutableFieldException.class)
                .hasMessageContaining("cpf");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void update_shouldFailWhenNewEmailIsTakenByAnotherCustomer() {
        Customer payload = Customer.createNew(
                "Maria", "taken@example.com", "12345678901", "secret123", "11999998888"
        );
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(existingId, payload))
                .isInstanceOf(DuplicateCustomerException.class)
                .hasMessageContaining("email");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void update_shouldFailWhenNewPhoneIsTakenByAnotherCustomer() {
        Customer payload = Customer.createNew(
                "Maria", "maria@example.com", "12345678901", "secret123", "11900000000"
        );
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByPhone("11900000000")).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(existingId, payload))
                .isInstanceOf(DuplicateCustomerException.class)
                .hasMessageContaining("phone");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void update_shouldNotCheckUniquenessWhenEmailAndPhoneAreUnchanged() {
        Customer payload = Customer.createNew(
                "Maria New", "maria@example.com", "12345678901", "secret123", "11999998888"
        );
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));
        when(customerRepository.save(existing)).thenReturn(existing);

        customerService.update(existingId, payload);

        verify(customerRepository, never()).existsByEmail(any());
        verify(customerRepository, never()).existsByPhone(any());
    }

    @Test
    void delete_shouldRemoveExisting() {
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));

        customerService.delete(existingId);

        verify(customerRepository, times(1)).deleteById(existingId);
    }

    @Test
    void delete_shouldThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.delete(id))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(customerRepository, never()).deleteById(any());
    }

}
