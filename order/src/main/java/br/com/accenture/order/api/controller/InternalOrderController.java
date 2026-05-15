package br.com.accenture.order.api.controller;

import br.com.accenture.order.api.dto.response.OrderResponse;
import br.com.accenture.order.application.dto.PaginatedResult;
import br.com.accenture.order.application.service.OrderService;
import br.com.accenture.order.domain.model.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/orders")
public class InternalOrderController {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<PaginatedResult<OrderResponse>> getOrdersByCustomer(
            @PathVariable UUID customerId,
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
}