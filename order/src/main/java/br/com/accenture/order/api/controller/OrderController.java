package br.com.accenture.order.api.controller;

import br.com.accenture.order.api.dto.request.OrderCreateRequest;
import br.com.accenture.order.api.dto.request.OrderItemRequest;
import br.com.accenture.order.api.dto.response.OrderResponse;
import br.com.accenture.order.application.dto.OrderItemCommand;
import br.com.accenture.order.application.service.OrderService;
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
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid OrderCreateRequest request) {

        List<OrderItemCommand> commands = request.items().stream()
                .map(OrderItemRequest::toCommand)
                .toList();

        Order savedOrder = orderService.createOrder(request.customerId(), commands);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedOrder.getId())
                .toUri();

        return ResponseEntity.created(location).body(new OrderResponse(savedOrder));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        Order order = orderService.findById(id);
        return ResponseEntity.ok(new OrderResponse(order));
    }
}