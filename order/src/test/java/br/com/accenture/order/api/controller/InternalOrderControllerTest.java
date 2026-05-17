package br.com.accenture.order.api.controller;

import br.com.accenture.order.application.dto.PaginatedResult;
import br.com.accenture.order.application.service.OrderService;
import br.com.accenture.order.domain.model.DeliveryAddress;
import br.com.accenture.order.domain.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalOrderController.class)
class InternalOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private static DeliveryAddress sampleAddress() {
        return new DeliveryAddress("Rua A", "10", null, "Bairro", "Cidade", "SP", "01001000");
    }

    @Test
    @DisplayName("Deve retornar 200 e resultado paginado com pedidos do cliente")
    void shouldReturn200WithPaginatedOrdersForCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        Order order = Order.createNew(customerId, sampleAddress());
        PaginatedResult<Order> paginatedResult = new PaginatedResult<>(List.of(order), 0, 10, 1, 1);

        when(orderService.findByCustomerId(customerId, 0, 10)).thenReturn(paginatedResult);

        mockMvc.perform(get("/internal/orders/customers/{customerId}", customerId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("Deve retornar 200 e lista vazia quando cliente nao tem pedidos")
    void shouldReturn200WithEmptyListWhenCustomerHasNoOrders() throws Exception {
        UUID customerId = UUID.randomUUID();
        PaginatedResult<Order> empty = new PaginatedResult<>(List.of(), 0, 10, 0, 0);
        when(orderService.findByCustomerId(customerId, 0, 10)).thenReturn(empty);

        mockMvc.perform(get("/internal/orders/customers/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Deve retornar 400 quando customerId nao for um UUID valido")
    void shouldReturn400WhenCustomerIdIsInvalid() throws Exception {
        mockMvc.perform(get("/internal/orders/customers/{customerId}", "nao-eh-uuid"))
                .andExpect(status().isBadRequest());
    }
}
