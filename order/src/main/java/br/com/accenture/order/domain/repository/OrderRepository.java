package br.com.accenture.order.domain.repository;

import br.com.accenture.order.application.dto.PaginatedResult;
import br.com.accenture.order.domain.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    void deleteById(UUID id);

    PaginatedResult<Order> findByCustomerId(UUID customerId, int page, int size);

}