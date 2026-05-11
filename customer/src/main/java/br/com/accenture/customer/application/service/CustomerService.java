package br.com.accenture.customer.application.service;

import br.com.accenture.customer.domain.event.CustomerCreatedEvent;
import br.com.accenture.customer.domain.exception.CustomerNotFoundException;
import br.com.accenture.customer.domain.exception.DuplicateCustomerException;
import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.domain.repository.CustomerRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CustomerService(CustomerRepository customerRepository, ApplicationEventPublisher eventPublisher) {
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Customer create(Customer customer) {
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new DuplicateCustomerException("email", customer.getEmail());
        }
        Customer saved = customerRepository.save(customer);
        eventPublisher.publishEvent(CustomerCreatedEvent.of(saved.getId(), saved.getEmail()));
        return saved;
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
