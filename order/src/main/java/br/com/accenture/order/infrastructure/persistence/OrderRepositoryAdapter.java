package br.com.accenture.order.infrastructure.persistence;

import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.domain.repository.OrderRepository;
import br.com.accenture.order.infrastructure.persistence.mapper.OrderPersistenceMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order save(Order order) {
        var entity = OrderPersistenceMapper.toEntity(order);
        var saved = jpaRepository.save(entity);
        return OrderPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpaRepository.findById(id).map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        return jpaRepository.findByCustomerId(customerId).stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

}