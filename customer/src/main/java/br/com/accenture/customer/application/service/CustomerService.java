package br.com.accenture.customer.application.service;

import br.com.accenture.customer.domain.exception.CustomerNotFoundException;
import br.com.accenture.customer.domain.exception.DuplicateCustomerException;
import br.com.accenture.customer.domain.exception.ImmutableFieldException;
import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Customer create(Customer customer) {
        validateUniqueness(customer);
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Customer findById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional
    public Customer update(UUID id, Customer updated) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        if (!existing.getCpf().equals(updated.getCpf())) {
            throw new ImmutableFieldException("cpf");
        }

        validateUniquenessOnUpdate(existing, updated);

        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        existing.setPassword(updated.getPassword());
        existing.setPhone(updated.getPhone());

        return customerRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (customerRepository.findById(id).isEmpty()) {
            throw new CustomerNotFoundException(id);
        }
        customerRepository.deleteById(id);
    }

    private void validateUniqueness(Customer customer) {
        if (customerRepository.existsByCpf(customer.getCpf())) {
            throw new DuplicateCustomerException("cpf", customer.getCpf());
        }
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new DuplicateCustomerException("email", customer.getEmail());
        }
        if (customerRepository.existsByPhone(customer.getPhone())) {
            throw new DuplicateCustomerException("phone", customer.getPhone());
        }
    }

    private void validateUniquenessOnUpdate(Customer existing, Customer updated) {
        if (!existing.getEmail().equals(updated.getEmail())
                && customerRepository.existsByEmail(updated.getEmail())) {
            throw new DuplicateCustomerException("email", updated.getEmail());
        }
        if (!existing.getPhone().equals(updated.getPhone())
                && customerRepository.existsByPhone(updated.getPhone())) {
            throw new DuplicateCustomerException("phone", updated.getPhone());
        }
    }

}
