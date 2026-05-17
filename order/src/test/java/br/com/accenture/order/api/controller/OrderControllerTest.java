package br.com.accenture.order.api.controller;

import br.com.accenture.order.api.dto.request.OrderCreateRequest;
import br.com.accenture.order.api.dto.request.OrderItemRequest;
import br.com.accenture.order.application.dto.PaginatedResult;
import br.com.accenture.order.application.service.OrderService;
import br.com.accenture.order.domain.enums.OrderStatus;
import br.com.accenture.order.domain.exception.OrderNotFoundException;
import br.com.accenture.order.domain.model.DeliveryAddress;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OrderService orderService;

    private static DeliveryAddress sampleAddress() {
        return new DeliveryAddress(
                "Rua das Flores", "123", "Apto 1", "Centro", "São Paulo", "SP", "01001000"
        );
    }

    @Test
    @DisplayName("Deve retornar 201 ao criar pedido enviando Header X-Customer-Id")
    void shouldReturn201WhenCreatingOrderWithValidDataAndHeader() throws Exception {
        UUID customerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID addressId = UUID.randomUUID();
        var itemRequest = new OrderItemRequest("SKU-123", 2, new BigDecimal("50.00"));
        var request = new OrderCreateRequest(addressId, List.of(itemRequest));

        Order mockOrder = Order.restore(UUID.randomUUID(), customerId, OrderStatus.PENDING,
                new BigDecimal("100.00"), List.of(), sampleAddress(), null, null, null);

        when(orderService.createOrder(eq(customerId), eq(addressId), anyList())).thenReturn(mockOrder);

        mockMvc.perform(post("/orders")
                        .header("X-Customer-Id", customerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.deliveryAddress.street").value("Rua das Flores"));
    }

    @Test
    @DisplayName("Deve retornar 400 ao tentar criar pedido sem o Header X-Customer-Id")
    void shouldReturn400WhenHeaderIsMissing() throws Exception {
        var itemRequest = new OrderItemRequest("SKU-123", 1, BigDecimal.TEN);
        var request = new OrderCreateRequest(UUID.randomUUID(), List.of(itemRequest));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 400 ao tentar criar pedido sem addressId no body")
    void shouldReturn400WhenAddressIdIsMissing() throws Exception {
        UUID customerId = UUID.randomUUID();
        var itemRequest = new OrderItemRequest("SKU-123", 1, BigDecimal.TEN);
        var request = new OrderCreateRequest(null, List.of(itemRequest));

        mockMvc.perform(post("/orders")
                        .header("X-Customer-Id", customerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 200 e resultado paginado ao buscar em /my-orders com Header")
    void shouldReturn200AndPaginatedResultForMyOrders() throws Exception {
        UUID customerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Order mockOrder = Order.createNew(customerId, sampleAddress());
        PaginatedResult<Order> paginatedResult = new PaginatedResult<>(List.of(mockOrder), 0, 10, 1, 1);

        when(orderService.findByCustomerId(customerId, 0, 10)).thenReturn(paginatedResult);

        mockMvc.perform(get("/orders/my-orders")
                        .header("X-Customer-Id", customerId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @DisplayName("Deve retornar 200 ao cancelar um pedido")
    void shouldReturn200WhenCancelingOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Order mockOrder = Order.restore(orderId, customerId, OrderStatus.CANCELED, BigDecimal.TEN, List.of(), sampleAddress(), null, null, null);

        when(orderService.cancelOrder(eq(orderId), anyString())).thenReturn(mockOrder);

        mockMvc.perform(patch("/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    @DisplayName("Deve retornar 200 e listar todos os pedidos paginados em GET /orders")
    void shouldReturn200WithPaginatedListOfAllOrders() throws Exception {
        Order order = Order.createNew(UUID.randomUUID(), sampleAddress());
        PaginatedResult<Order> result = new PaginatedResult<>(List.of(order), 0, 10, 1, 1);
        when(orderService.findAll(0, 10)).thenReturn(result);

        mockMvc.perform(get("/orders").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Deve retornar 200 ao buscar pedido por id quando o cliente e o dono")
    void shouldReturn200WhenCustomerOwnsTheOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Order mockOrder = Order.restore(orderId, customerId, OrderStatus.PENDING, BigDecimal.TEN, List.of(),
                sampleAddress(), null, null, null);

        when(orderService.findById(orderId)).thenReturn(mockOrder);

        mockMvc.perform(get("/orders/{id}", orderId)
                        .header("X-Customer-Id", customerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()));
    }

    @Test
    @DisplayName("Deve retornar 200 ao buscar pedido por id quando nao ha headers de identificacao")
    void shouldReturn200WhenNoCustomerHeader() throws Exception {
        UUID orderId = UUID.randomUUID();
        Order mockOrder = Order.restore(orderId, UUID.randomUUID(), OrderStatus.PENDING, BigDecimal.TEN, List.of(),
                sampleAddress(), null, null, null);

        when(orderService.findById(orderId)).thenReturn(mockOrder);

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()));
    }

    @Test
    @DisplayName("Deve lancar OrderNotFoundException quando o cliente nao e dono e nao e ADMIN")
    void shouldThrowOrderNotFoundWhenCustomerIsNotOwnerAndNotAdmin() {
        UUID orderId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();
        Order mockOrder = Order.restore(orderId, ownerId, OrderStatus.PENDING, BigDecimal.TEN, List.of(),
                sampleAddress(), null, null, null);

        when(orderService.findById(orderId)).thenReturn(mockOrder);

        assertThatThrownBy(() -> mockMvc.perform(get("/orders/{id}", orderId)
                        .header("X-Customer-Id", otherCustomerId.toString())
                        .header("X-User-Roles", "CUSTOMER")))
                .rootCause()
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Deve retornar 200 ao buscar pedido de outro cliente quando o usuario possui role ADMIN")
    void shouldReturn200WhenAdminAccessesAnotherCustomerOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Order mockOrder = Order.restore(orderId, ownerId, OrderStatus.PENDING, BigDecimal.TEN, List.of(),
                sampleAddress(), null, null, null);

        when(orderService.findById(orderId)).thenReturn(mockOrder);

        mockMvc.perform(get("/orders/{id}", orderId)
                        .header("X-Customer-Id", adminId.toString())
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()));
    }

    @Test
    @DisplayName("Deve lancar OrderNotFoundException ao tentar cancelar pedido de outro cliente sem role ADMIN")
    void shouldThrowOrderNotFoundWhenCancelingOrderFromAnotherCustomer() {
        UUID orderId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();
        Order mockOrder = Order.restore(orderId, ownerId, OrderStatus.PENDING, BigDecimal.TEN, List.of(),
                sampleAddress(), null, null, null);

        when(orderService.findById(orderId)).thenReturn(mockOrder);

        assertThatThrownBy(() -> mockMvc.perform(patch("/orders/{id}/cancel", orderId)
                        .header("X-Customer-Id", otherCustomerId.toString())
                        .header("X-User-Roles", "CUSTOMER")))
                .rootCause()
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Deve propagar OrderNotFoundException lancada pelo service quando o pedido nao existe")
    void shouldPropagateOrderNotFoundExceptionFromService() {
        UUID orderId = UUID.randomUUID();
        when(orderService.findById(orderId)).thenThrow(new OrderNotFoundException(orderId));

        assertThatThrownBy(() -> mockMvc.perform(get("/orders/{id}", orderId)))
                .rootCause()
                .isInstanceOf(OrderNotFoundException.class);
    }
}
