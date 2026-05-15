package br.com.accenture.customer.application.service;

import br.com.accenture.customer.domain.exception.CustomerNotFoundException;
import br.com.accenture.customer.domain.exception.DuplicateCustomerException;
import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;
import br.com.accenture.customer.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public Customer findById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public PageResult<Customer> findAll(PageRequest pageRequest) {
        return customerRepository.findAll(pageRequest);
    }

    @Transactional
    public Customer create(Customer customer) {
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new DuplicateCustomerException("email", customer.getEmail());
        }
        if (customerRepository.existsByCpf(customer.getCpf())) {
            throw new DuplicateCustomerException("cpf", customer.getCpf());
        }
        if (customerRepository.existsByPhone(customer.getPhone())) {
            throw new DuplicateCustomerException("phone", customer.getPhone());
        }
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(UUID id, Customer updated) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        validateUniquenessOnUpdate(existing, updated);

        existing.updateProfile(
                updated.getName(),
                updated.getCpf(),
                updated.getPhone()
        );

        return customerRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        customerRepository.deleteById(existing.getId());
    }

    private void validateUniquenessOnUpdate(Customer existing, Customer updated) {
        if (updated.getCpf() != null
                && !updated.getCpf().equals(existing.getCpf())
                && customerRepository.existsByCpf(updated.getCpf())) {
            throw new DuplicateCustomerException("cpf", updated.getCpf());
        }
        if (updated.getPhone() != null
                && !updated.getPhone().equals(existing.getPhone())
                && customerRepository.existsByPhone(updated.getPhone())) {
            throw new DuplicateCustomerException("phone", updated.getPhone());
        }
    }

}
