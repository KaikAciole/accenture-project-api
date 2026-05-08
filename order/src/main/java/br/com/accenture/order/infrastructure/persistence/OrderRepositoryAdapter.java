package br.com.accenture.order.infrastructure.persistence;

import br.com.accenture.order.application.dto.PaginatedResult;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.domain.repository.OrderRepository;
import br.com.accenture.order.infrastructure.persistence.entity.OrderJpaEntity;
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
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public PaginatedResult<Order> findByCustomerId(String customerId, int page, int size) {
        org.springframework.data.domain.PageRequest pageRequest =
                org.springframework.data.domain.PageRequest.of(page, size);

        org.springframework.data.domain.Page<OrderJpaEntity> entityPage =
                jpaRepository.findByCustomerId(customerId, pageRequest);

        List<Order> domainOrders = entityPage.getContent().stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();

        return new PaginatedResult<>(
                domainOrders,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

}