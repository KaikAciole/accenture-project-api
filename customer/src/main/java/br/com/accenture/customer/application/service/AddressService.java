package br.com.accenture.customer.application.service;

import br.com.accenture.customer.domain.exception.AddressNotFoundException;
import br.com.accenture.customer.domain.exception.CustomerNotFoundException;
import br.com.accenture.customer.domain.exception.ImmutableFieldException;
import br.com.accenture.customer.domain.model.Address;
import br.com.accenture.customer.domain.repository.AddressRepository;
import br.com.accenture.customer.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;

    public AddressService(AddressRepository addressRepository, CustomerRepository customerRepository) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Address create(Address address) {
        validateCustomerExists(address.getCustomerId());
        return addressRepository.save(address);
    }

    @Transactional(readOnly = true)
    public List<Address> findAll() {
        return addressRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Address findById(UUID id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new AddressNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Address> findByCustomerId(UUID customerId) {
        validateCustomerExists(customerId);
        return addressRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Address update(UUID id, Address updated) {
        Address existing = addressRepository.findById(id)
                .orElseThrow(() -> new AddressNotFoundException(id));

        if (!existing.getCustomerId().equals(updated.getCustomerId())) {
            throw new ImmutableFieldException("customerId");
        }

        existing.update(
                updated.getStreet(),
                updated.getNumber(),
                updated.getComplement(),
                updated.getNeighborhood(),
                updated.getCity(),
                updated.getState(),
                updated.getZipCode()
        );

        return addressRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (addressRepository.findById(id).isEmpty()) {
            throw new AddressNotFoundException(id);
        }
        addressRepository.deleteById(id);
    }

    private void validateCustomerExists(UUID customerId) {
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException(customerId);
        }
    }

}
