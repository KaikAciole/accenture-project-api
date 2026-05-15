package br.com.accenture.order.application.service;

import br.com.accenture.order.application.dto.OrderItemCommand;
import br.com.accenture.order.application.dto.PaginatedResult;
import br.com.accenture.order.application.publisher.OrderEventPublisher;
import br.com.accenture.order.domain.enums.OrderStatus;
import br.com.accenture.order.domain.exception.InsufficientStockException;
import br.com.accenture.order.domain.exception.OrderNotFoundException;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.domain.model.OrderItem;
import br.com.accenture.order.domain.repository.OrderRepository;
import br.com.accenture.order.infrastructure.feign.InventoryClient;
import br.com.accenture.order.infrastructure.feign.dto.ProductAvailabilityItemRequest;
import br.com.accenture.order.infrastructure.feign.dto.ProductAvailabilityItemResponse;
import br.com.accenture.order.infrastructure.feign.dto.ProductAvailabilityRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final InventoryClient inventoryClient;

    @Value("${api.security.internal.secret:senha-secreta-microsservicos-1234}")
    private String internalSecret;

    public OrderService(OrderRepository orderRepository,
                        OrderEventPublisher eventPublisher,
                        InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.inventoryClient = inventoryClient;
    }

    @Transactional
    public Order createOrder(UUID customerId, List<OrderItemCommand> itemsRequest) {

        List<ProductAvailabilityItemRequest> checkItems = itemsRequest.stream()
                .map(item -> new ProductAvailabilityItemRequest(item.sku(), item.quantity()))
                .toList();

        List<ProductAvailabilityItemResponse> availabilityList = inventoryClient.checkAvailability(
                new ProductAvailabilityRequest(checkItems), internalSecret);

        for (ProductAvailabilityItemResponse response : availabilityList) {
            if (!response.available()) {
                throw new InsufficientStockException(
                        String.format("Estoque insuficiente ou produto não encontrado para o SKU: %s", response.sku())
                );
            }
        }

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

    @Transactional(readOnly = true)
    public PaginatedResult<Order> findByCustomerId(UUID customerId, int page, int size) {
        return orderRepository.findByCustomerId(customerId, page, size);
    }

    @Transactional
    public Order markOrderAsReserved(UUID id) {
        Order order = findById(id);
        order.markAsReserved();
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishOrderReservedEvent(savedOrder);
        return savedOrder;
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
    public Order refundOrder(UUID id, String reason) {
        Order order = findById(id);
        order.markAsRefunded();
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(UUID id, String reason) {
        Order order = findById(id);

        if (order.getStatus() == OrderStatus.CANCELED) {
            return order;
        }

        try {
            order.cancel();
            Order savedOrder = orderRepository.save(order);
            eventPublisher.publishOrderCanceledEvent(savedOrder, reason);
            return savedOrder;

        } catch (IllegalStateException e) {
            if (order.getStatus() == OrderStatus.PAID) {
                eventPublisher.publishOrderCanceledEvent(order, "Cancelamento de pedido já pago. Estorno em processamento: " + reason);
                return order;
            }
            throw e;
        }
    }
}