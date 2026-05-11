package br.com.accenture.customer.application.service;

import br.com.accenture.customer.domain.event.CustomerCreatedEvent;
import br.com.accenture.customer.domain.exception.CustomerNotFoundException;
import br.com.accenture.customer.domain.exception.DuplicateCustomerException;
import br.com.accenture.customer.domain.exception.ImmutableFieldException;
import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.domain.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
    void create_shouldPersistAndPublishEventWhenEmailIsUnique() {
        when(customerRepository.existsByEmail(newCustomer.getEmail())).thenReturn(false);
        when(customerRepository.save(newCustomer)).thenReturn(existing);

        Customer saved = customerService.create(newCustomer);

        assertThat(saved.getId()).isEqualTo(existingId);
        verify(customerRepository).save(newCustomer);

        ArgumentCaptor<CustomerCreatedEvent> captor = ArgumentCaptor.forClass(CustomerCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        CustomerCreatedEvent event = captor.getValue();
        assertThat(event.customerId()).isEqualTo(existingId);
        assertThat(event.email()).isEqualTo("maria@example.com");
        assertThat(event.eventType()).isEqualTo("customer.created");
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void create_shouldFailAndNotPublishEventWhenEmailAlreadyExists() {
        when(customerRepository.existsByEmail(newCustomer.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(newCustomer))
                .isInstanceOf(DuplicateCustomerException.class)
                .hasMessageContaining("email");

        verify(customerRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
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
    void update_shouldNotPublishCustomerCreatedEvent() {
        Customer payload = Customer.restore(null, "Maria New", null, null, null, null, null);
        when(customerRepository.findById(existingId)).thenReturn(Optional.of(existing));
        when(customerRepository.save(existing)).thenReturn(existing);

        customerService.update(existingId, payload);

        verify(eventPublisher, never()).publishEvent(any());
    }

}
