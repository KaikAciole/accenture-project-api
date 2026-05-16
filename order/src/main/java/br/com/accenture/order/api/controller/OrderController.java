package br.com.accenture.order.api.controller;

import br.com.accenture.order.api.dto.request.OrderCreateRequest;
import br.com.accenture.order.api.dto.request.OrderItemRequest;
import br.com.accenture.order.api.dto.response.OrderResponse;
import br.com.accenture.order.application.dto.OrderItemCommand;
import br.com.accenture.order.application.dto.PaginatedResult;
import br.com.accenture.order.application.service.OrderService;
import br.com.accenture.order.domain.exception.OrderNotFoundException;
import br.com.accenture.order.domain.model.Order;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-Customer-Id") UUID customerId,
            @RequestBody @Valid OrderCreateRequest request) {

        List<OrderItemCommand> commands = request.items().stream()
                .map(OrderItemRequest::toCommand)
                .toList();

        Order savedOrder = orderService.createOrder(customerId, request.addressId(), commands);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedOrder.getId())
                .toUri();

        return ResponseEntity.created(location).body(new OrderResponse(savedOrder));
    }

    @GetMapping
    public ResponseEntity<PaginatedResult<OrderResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PaginatedResult<Order> paginatedOrders = orderService.findAll(page, size);

        List<OrderResponse> responses = paginatedOrders.data().stream()
                .map(OrderResponse::new)
                .toList();

        PaginatedResult<OrderResponse> responsePage = new PaginatedResult<>(
                responses,
                paginatedOrders.page(),
                paginatedOrders.size(),
                paginatedOrders.totalElements(),
                paginatedOrders.totalPages()
        );

        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Customer-Id", required = false) UUID customerId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        Order order = orderService.findById(id);
        validateOwnership(order, customerId, roles);
        return ResponseEntity.ok(new OrderResponse(order));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<PaginatedResult<OrderResponse>> getMyOrders(
            @RequestHeader("X-Customer-Id") UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PaginatedResult<Order> paginatedOrders = orderService.findByCustomerId(customerId, page, size);

        List<OrderResponse> responses = paginatedOrders.data().stream()
                .map(OrderResponse::new)
                .toList();

        PaginatedResult<OrderResponse> responsePage = new PaginatedResult<>(
                responses,
                paginatedOrders.page(),
                paginatedOrders.size(),
                paginatedOrders.totalElements(),
                paginatedOrders.totalPages()
        );

        return ResponseEntity.ok(responsePage);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Customer-Id", required = false) UUID customerId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (customerId != null) {
            validateOwnership(orderService.findById(id), customerId, roles);
        }
        Order canceledOrder = orderService.cancelOrder(id, "Cancelamento solicitado pelo cliente via API");
        return ResponseEntity.ok(new OrderResponse(canceledOrder));
    }

    private void validateOwnership(Order order, UUID customerId, String roles) {
        if (customerId == null) {
            return;
        }
        boolean isAdmin = roles != null && roles.contains("ADMIN");
        if (!isAdmin && !order.getCustomerId().equals(customerId)) {
            throw new OrderNotFoundException(order.getId());
        }
    }
}