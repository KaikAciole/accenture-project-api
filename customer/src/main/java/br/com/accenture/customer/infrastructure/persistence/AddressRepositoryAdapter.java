package br.com.accenture.customer.infrastructure.persistence;

import br.com.accenture.customer.domain.model.Address;
import br.com.accenture.customer.domain.repository.AddressRepository;
import br.com.accenture.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import br.com.accenture.customer.infrastructure.persistence.mapper.AddressPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AddressRepositoryAdapter implements AddressRepository {

    private final AddressJpaRepository jpaRepository;
    private final EntityManager entityManager;

    public AddressRepositoryAdapter(AddressJpaRepository jpaRepository, EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Address save(Address address) {
        CustomerJpaEntity customerRef = entityManager.getReference(CustomerJpaEntity.class, address.getCustomerId());
        var entity = AddressPersistenceMapper.toEntity(address, customerRef);
        var saved = jpaRepository.save(entity);
        return AddressPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Address> findById(UUID id) {
        return jpaRepository.findById(id).map(AddressPersistenceMapper::toDomain);
    }

    @Override
    public Page<Address> findByCustomerId(UUID customerId, Pageable pageable) {
        return jpaRepository.findByCustomerId(customerId, pageable)
                .map(AddressPersistenceMapper::toDomain);
    }

    @Override
    public List<Address> findAll() {
        return jpaRepository.findAll().stream()
                .map(AddressPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

}
