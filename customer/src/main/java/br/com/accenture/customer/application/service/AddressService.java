package br.com.accenture.customer.application.service;

import br.com.accenture.customer.domain.exception.AddressNotFoundException;
import br.com.accenture.customer.domain.exception.CustomerNotFoundException;
import br.com.accenture.customer.domain.model.Address;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;
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
    public Address findByCustomerAndId(UUID customerId, UUID addressId) {
        return loadOwned(customerId, addressId);
    }

    @Transactional(readOnly = true)
    public PageResult<Address> findByCustomerId(UUID customerId, PageRequest pageRequest) {
        validateCustomerExists(customerId);
        return addressRepository.findByCustomerId(customerId, pageRequest);
    }

    @Transactional
    public Address update(UUID customerId, UUID addressId, Address updated) {
        Address existing = loadOwned(customerId, addressId);
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
    public void delete(UUID customerId, UUID addressId) {
        loadOwned(customerId, addressId);
        addressRepository.deleteById(addressId);
    }

    private Address loadOwned(UUID customerId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));
        if (!address.getCustomerId().equals(customerId)) {
            throw new AddressNotFoundException(addressId);
        }
        return address;
    }

    private void validateCustomerExists(UUID customerId) {
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException(customerId);
        }
    }

}
