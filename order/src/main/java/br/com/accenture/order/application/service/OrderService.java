package br.com.accenture.order.application.service;

import br.com.accenture.order.application.dto.OrderItemCommand;
import br.com.accenture.order.application.dto.PaginatedResult;
import br.com.accenture.order.domain.exception.OrderNotFoundException;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.domain.model.OrderItem;
import br.com.accenture.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order createOrder(String customerId, List<OrderItemCommand> itemsCommand) {

        Order newOrder = Order.createNew(customerId);

        for (OrderItemCommand cmd : itemsCommand) {
            OrderItem item = OrderItem.createNew(cmd.sku(), cmd.quantity(), cmd.unitPrice());
            newOrder.addItem(item);
        }

        return orderRepository.save(newOrder);
    }

    @Transactional(readOnly = true)
    public Order findById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public void confirmOrderReservation(UUID id) {
        Order order = findById(id);
        order.confirmReservation();

        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public PaginatedResult<Order> findByCustomerId(String customerId, int page, int size) {
        return orderRepository.findByCustomerId(customerId, page, size);
    }
}