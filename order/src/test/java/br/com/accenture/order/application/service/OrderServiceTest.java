package br.com.accenture.order.application.service;

import br.com.accenture.order.application.dto.OrderItemCommand;
import br.com.accenture.order.application.dto.PaginatedResult;
import br.com.accenture.order.application.publisher.OrderEventPublisher;
import br.com.accenture.order.domain.enums.OrderStatus;
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

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Deve orquestrar a criacao de um pedido, salvar e publicar evento")
    void shouldCreateOrderAndSaveToRepositoryAndPublishEvent() {
        UUID customerId = UUID.randomUUID();
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
        verify(eventPublisher, times(1)).publishOrderCreatedEvent(any(Order.class));
    }

    @Test
    @DisplayName("Deve buscar pedido pelo ID com sucesso")
    void shouldFindOrderByIdSuccessfully() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Order mockOrder = Order.createNew(customerId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        Order foundOrder = orderService.findById(orderId);

        assertThat(foundOrder).isNotNull();
        assertThat(foundOrder.getCustomerId()).isEqualTo(customerId);

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("Deve retornar o resultado paginado ao buscar historico do cliente")
    void shouldReturnPaginatedResultForCustomerHistory() {
        UUID customerId = UUID.randomUUID();
        Order dummyOrder = Order.createNew(customerId);

        PaginatedResult<Order> expectedPage = new PaginatedResult<>(
                List.of(dummyOrder), 0, 10, 1, 1
        );

        when(orderRepository.findByCustomerId(customerId, 0, 10)).thenReturn(expectedPage);

        PaginatedResult<Order> result = orderService.findByCustomerId(customerId, 0, 10);

        assertThat(result).isNotNull();
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.data()).hasSize(1);

        verify(orderRepository, times(1)).findByCustomerId(customerId, 0, 10);
    }

    @Test
    @DisplayName("Deve marcar o pedido como RESERVADO, salvar e publicar evento para iniciar pagamento")
    void shouldMarkOrderAsReservedAndPublishEvent() {
        UUID orderId = UUID.randomUUID();
        Order mockOrder = Order.createNew(UUID.randomUUID());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updatedOrder = orderService.markOrderAsReserved(orderId);

        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.RESERVED);

        verify(orderRepository, times(1)).save(mockOrder);
        verify(eventPublisher, times(1)).publishOrderReservedEvent(mockOrder);
    }

    @Test
    @DisplayName("Deve marcar o pedido como PAGO, salvar e publicar evento (transição de RESERVED)")
    void shouldMarkOrderAsPaidAndPublishEvent() {
        UUID orderId = UUID.randomUUID();
        Order mockOrder = Order.createNew(UUID.randomUUID());

        mockOrder.markAsReserved();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updatedOrder = orderService.markOrderAsPaid(orderId);

        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);

        verify(orderRepository, times(1)).save(mockOrder);
        verify(eventPublisher, times(1)).publishOrderPaidEvent(mockOrder);
    }

    @Test
    @DisplayName("Deve disparar evento de cancelamento sem alterar status se o pedido ja estiver PAGO")
    void shouldPublishCancelEventWhenOrderIsAlreadyPaid() {
        UUID orderId = UUID.randomUUID();
        Order mockOrder = Order.createNew(UUID.randomUUID());

        mockOrder.markAsReserved();
        mockOrder.markAsPaid();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        Order result = orderService.cancelOrder(orderId, "Cliente solicitou estorno");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, times(1)).publishOrderCanceledEvent(eq(mockOrder), anyString());
    }

    @Test
    @DisplayName("Deve marcar o pedido como REFUNDED após o estorno no payment")
    void shouldMarkOrderAsRefunded() {
        UUID orderId = UUID.randomUUID();
        Order mockOrder = Order.createNew(UUID.randomUUID());
        mockOrder.markAsReserved();
        mockOrder.markAsPaid();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updatedOrder = orderService.refundOrder(orderId, "Estorno confirmado pelo Gateway");

        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        verify(orderRepository, times(1)).save(mockOrder);
    }

    @Test
    @DisplayName("Deve cancelar o pedido, salvar e publicar evento com motivo")
    void shouldCancelOrderAndPublishEvent() {
        UUID orderId = UUID.randomUUID();
        Order mockOrder = Order.createNew(UUID.randomUUID());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updatedOrder = orderService.cancelOrder(orderId, "Falta de estoque");

        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);

        verify(orderRepository, times(1)).save(mockOrder);
        verify(eventPublisher, times(1)).publishOrderCanceledEvent(mockOrder, "Falta de estoque");
    }

    @Test
    @DisplayName("Deve lancar excecao ao tentar alterar status de pedido inexistente")
    void shouldThrowExceptionWhenChangingStatusForNonExistentOrder() {
        UUID fakeId = UUID.randomUUID();

        when(orderRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.markOrderAsPaid(fakeId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishOrderPaidEvent(any());
    }
}