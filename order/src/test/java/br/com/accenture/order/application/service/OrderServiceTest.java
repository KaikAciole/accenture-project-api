package br.com.accenture.order.application.service;

import br.com.accenture.order.application.dto.OrderItemCommand;
import br.com.accenture.order.application.dto.PaginatedResult;
import br.com.accenture.order.domain.exception.OrderNotFoundException;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.domain.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Deve orquestrar a criacao de um pedido com itens e chamar o repositorio")
    void shouldCreateOrderAndSaveToRepository() {
        String customerId = "customer-007";
        List<OrderItemCommand> commands = List.of(
                new OrderItemCommand("LAPTOP-XYZ", 1, new BigDecimal("5000.00"))
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order savedOrder = orderService.createOrder(customerId, commands);

        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getCustomerId()).isEqualTo(customerId);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(savedOrder.getItems()).hasSize(1);

        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve buscar pedido pelo ID com sucesso")
    void shouldFindOrderByIdSuccessfully() {
        UUID orderId = UUID.randomUUID();
        Order mockOrder = Order.createNew("customer-007");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        Order foundOrder = orderService.findById(orderId);

        assertThat(foundOrder).isNotNull();
        assertThat(foundOrder.getCustomerId()).isEqualTo("customer-007");

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("Deve lancar OrderNotFoundException quando tentar buscar ID inexistente")
    void shouldThrowExceptionWhenFindingNonExistentOrder() {
        UUID fakeId = UUID.randomUUID();
        when(orderRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(fakeId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, times(1)).findById(fakeId);
    }

    @Test
    @DisplayName("Deve retornar o resultado paginado ao buscar historico do cliente")
    void shouldReturnPaginatedResultForCustomerHistory() {
        String customerId = "customer-007";
        Order dummyOrder = Order.createNew(customerId);

        PaginatedResult<Order> expectedPage = new PaginatedResult<>(
                List.of(dummyOrder), 0, 10, 1, 1
        );

        when(orderRepository.findByCustomerId(customerId, 0, 10)).thenReturn(expectedPage);

        PaginatedResult<Order> result = orderService.findByCustomerId(customerId, 0, 10);

        assertThat(result).isNotNull();
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).getCustomerId()).isEqualTo(customerId);

        verify(orderRepository, times(1)).findByCustomerId(customerId, 0, 10);
    }

    @Test
    @DisplayName("Deve confirmar reserva do pedido chamando o repositorio")
    void shouldConfirmOrderReservationAndSave() {
        UUID orderId = UUID.randomUUID();
        Order mockOrder = Order.createNew("customer-007");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        orderService.confirmOrderReservation(orderId);

        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(mockOrder);
    }

    @Test
    @DisplayName("Deve lancar excecao ao tentar confirmar reserva de pedido inexistente")
    void shouldThrowExceptionWhenConfirmingReservationForNonExistentOrder() {
        UUID fakeId = UUID.randomUUID();

        when(orderRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.confirmOrderReservation(fakeId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }
}