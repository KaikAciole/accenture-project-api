package br.com.accenture.order.api.controller;

import br.com.accenture.order.api.dto.request.OrderCreateRequest;
import br.com.accenture.order.api.dto.request.OrderItemRequest;
import br.com.accenture.order.application.dto.PaginatedResult;
import br.com.accenture.order.application.service.OrderService;
import br.com.accenture.order.domain.enums.OrderStatus;
import br.com.accenture.order.domain.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("Deve retornar 201 ao criar pedido com dados validos")
    void shouldReturn201WhenCreatingOrderWithValidData() throws Exception {
        UUID customerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        var itemRequest = new OrderItemRequest("SKU-123", 2, new BigDecimal("50.00"));
        var request = new OrderCreateRequest(customerId, List.of(itemRequest));

        Order mockOrder = Order.restore(UUID.randomUUID(), customerId, OrderStatus.PENDING,
                new BigDecimal("100.00"), List.of(), null, null);

        when(orderService.createOrder(eq(customerId), anyList())).thenReturn(mockOrder);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()));
    }

    @Test
    @DisplayName("Deve retornar 400 ao tentar criar pedido com lista de itens vazia")
    void shouldReturn400WhenItemsListIsEmpty() throws Exception {
        UUID customerId = UUID.randomUUID();
        var request = new OrderCreateRequest(customerId, List.of());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 400 ao tentar criar pedido com customerId nulo")
    void shouldReturn400WhenCustomerIdIsNull() throws Exception {
        var itemRequest = new OrderItemRequest("SKU-123", 1, BigDecimal.TEN);
        var request = new OrderCreateRequest(null, List.of(itemRequest));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 200 e resultado paginado ao buscar por cliente")
    void shouldReturn200AndPaginatedResult() throws Exception {
        UUID customerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Order mockOrder = Order.createNew(customerId);
        PaginatedResult<Order> paginatedResult = new PaginatedResult<>(List.of(mockOrder), 0, 10, 1, 1);

        when(orderService.findByCustomerId(customerId, 0, 10)).thenReturn(paginatedResult);

        mockMvc.perform(get("/orders/customers/{customerId}", customerId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }
}