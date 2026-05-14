package br.com.accenture.customer.infrastructure.persistence;

import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;
import br.com.accenture.customer.domain.repository.CustomerRepository;
import br.com.accenture.customer.infrastructure.pagination.SpringPaginationConverter;
import br.com.accenture.customer.infrastructure.persistence.mapper.CustomerPersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpaRepository;

    public CustomerRepositoryAdapter(CustomerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Customer save(Customer customer) {
        var entity = CustomerPersistenceMapper.toEntity(customer);
        var saved = jpaRepository.save(entity);
        return CustomerPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return jpaRepository.findById(id).map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<Customer> findAll(PageRequest pageRequest) {
        Pageable pageable = SpringPaginationConverter.toPageable(pageRequest);
        Page<Customer> page = jpaRepository.findAll(pageable)
                .map(CustomerPersistenceMapper::toDomain);
        return SpringPaginationConverter.toPageResult(page);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return jpaRepository.existsByCpf(cpf);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return jpaRepository.existsByPhone(phone);
    }

}
