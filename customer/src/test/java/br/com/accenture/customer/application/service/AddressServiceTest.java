package br.com.accenture.customer.application.service;

import br.com.accenture.customer.domain.exception.AddressNotFoundException;
import br.com.accenture.customer.domain.exception.CustomerNotFoundException;
import br.com.accenture.customer.domain.model.Address;
import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;
import br.com.accenture.customer.domain.repository.AddressRepository;
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
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private AddressService addressService;

    private UUID customerId;
    private UUID addressId;
    private Customer existingCustomer;
    private Address existingAddress;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        existingCustomer = Customer.restore(
                customerId, "Maria", "maria@example.com", "12345678901", "secret123", "11999998888",
                Instant.now(), Instant.now()
        );
        existingAddress = Address.restore(
                addressId, customerId, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000",
                Instant.now(), Instant.now()
        );
    }

    @Test
    void create_shouldPersistWhenCustomerExists() {
        Address newAddress = Address.createNew(
                customerId, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000"
        );
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(addressRepository.save(newAddress)).thenReturn(existingAddress);

        Address saved = addressService.create(newAddress);

        assertThat(saved.getId()).isEqualTo(addressId);
    }

    @Test
    void create_shouldFailWhenCustomerDoesNotExist() {
        Address newAddress = Address.createNew(
                customerId, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000"
        );
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.create(newAddress))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(addressRepository, never()).save(any());
    }

    @Test
    void findAll_shouldReturnAllAddresses() {
        when(addressRepository.findAll()).thenReturn(List.of(existingAddress));

        List<Address> all = addressService.findAll();

        assertThat(all).containsExactly(existingAddress);
    }

    @Test
    void findByCustomerAndId_shouldReturnAddressWhenOwnedByCustomer() {
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(existingAddress));

        Address found = addressService.findByCustomerAndId(customerId, addressId);

        assertThat(found).isSameAs(existingAddress);
    }

    @Test
    void findByCustomerAndId_shouldThrowWhenAddressMissing() {
        when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.findByCustomerAndId(customerId, addressId))
                .isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    void findByCustomerAndId_shouldThrowWhenAddressBelongsToAnotherCustomer() {
        UUID anotherCustomer = UUID.randomUUID();
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(existingAddress));

        assertThatThrownBy(() -> addressService.findByCustomerAndId(anotherCustomer, addressId))
                .isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    void findByCustomerId_shouldDelegateToRepositoryWhenCustomerExists() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageResult<Address> page = new PageResult<>(List.of(existingAddress), 0, 10, 1, 1);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(addressRepository.findByCustomerId(customerId, pageRequest)).thenReturn(page);

        PageResult<Address> result = addressService.findByCustomerId(customerId, pageRequest);

        assertThat(result.content()).containsExactly(existingAddress);
    }

    @Test
    void findByCustomerId_shouldThrowWhenCustomerMissing() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.findByCustomerId(customerId, pageRequest))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(addressRepository, never()).findByCustomerId(any(), any());
    }

    @Test
    void update_shouldPersistChangesOnOwnedAddress() {
        Address payload = Address.createNew(
                customerId, "Rua B", "200", "Casa 2", "Bairro Novo", "Rio", "RJ", "22222222"
        );
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(existingAddress)).thenReturn(existingAddress);

        Address updated = addressService.update(customerId, addressId, payload);

        assertThat(updated.getStreet()).isEqualTo("Rua B");
        assertThat(updated.getCity()).isEqualTo("Rio");
        assertThat(updated.getState()).isEqualTo("RJ");
        verify(addressRepository).save(existingAddress);
    }

    @Test
    void update_shouldThrowWhenAddressMissing() {
        Address payload = Address.createNew(
                customerId, "Rua B", "200", null, "Centro", "São Paulo", "SP", "01001000"
        );
        when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.update(customerId, addressId, payload))
                .isInstanceOf(AddressNotFoundException.class);

        verify(addressRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowWhenAddressBelongsToAnotherCustomer() {
        UUID anotherCustomer = UUID.randomUUID();
        Address payload = Address.createNew(
                anotherCustomer, "Rua B", "200", null, "Centro", "São Paulo", "SP", "01001000"
        );
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(existingAddress));

        assertThatThrownBy(() -> addressService.update(anotherCustomer, addressId, payload))
                .isInstanceOf(AddressNotFoundException.class);

        verify(addressRepository, never()).save(any());
    }

    @Test
    void delete_shouldRemoveOwnedAddress() {
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(existingAddress));

        addressService.delete(customerId, addressId);

        verify(addressRepository).deleteById(addressId);
    }

    @Test
    void delete_shouldThrowWhenAddressMissing() {
        when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.delete(customerId, addressId))
                .isInstanceOf(AddressNotFoundException.class);

        verify(addressRepository, never()).deleteById(any());
    }

    @Test
    void delete_shouldThrowWhenAddressBelongsToAnotherCustomer() {
        UUID anotherCustomer = UUID.randomUUID();
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(existingAddress));

        assertThatThrownBy(() -> addressService.delete(anotherCustomer, addressId))
                .isInstanceOf(AddressNotFoundException.class);

        verify(addressRepository, never()).deleteById(any());
    }

}
