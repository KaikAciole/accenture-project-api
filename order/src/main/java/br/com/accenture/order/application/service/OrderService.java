package br.com.accenture.order.application.service;

import br.com.accenture.order.application.dto.OrderItemCommand;
import br.com.accenture.order.application.dto.PaginatedResult;
import br.com.accenture.order.domain.exception.OrderNotFoundException;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.domain.model.OrderItem;
import br.com.accenture.order.domain.repository.OrderRepository;
import br.com.accenture.order.application.publisher.OrderEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order createOrder(UUID customerId, List<OrderItemCommand> itemsRequest) {

        Order newOrder = Order.createNew(customerId);

        itemsRequest.forEach(request -> {
            OrderItem item = OrderItem.createNew(request.sku(), request.quantity(), request.unitPrice());
            newOrder.addItem(item);
        });

        Order savedOrder = orderRepository.save(newOrder);
        eventPublisher.publishOrderCreatedEvent(savedOrder);

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Order findById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public Order markOrderAsPaid(UUID id) {
        Order order = findById(id);
        order.markAsPaid();
        Order savedOrder = orderRepository.save(order);

        eventPublisher.publishOrderPaidEvent(savedOrder);
        return savedOrder;
    }

    @Transactional
    public Order cancelOrder(UUID id, String reason) {
        Order order = findById(id);

        order.cancel();
        Order savedOrder = orderRepository.save(order);

        eventPublisher.publishOrderCanceledEvent(savedOrder, reason);
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public PaginatedResult<Order> findByCustomerId(UUID customerId, int page, int size) {
        return orderRepository.findByCustomerId(customerId, page, size);
    }
}