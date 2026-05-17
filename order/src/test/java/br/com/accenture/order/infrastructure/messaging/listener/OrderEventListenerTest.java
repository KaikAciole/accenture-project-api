package br.com.accenture.order.infrastructure.messaging.listener;

import br.com.accenture.order.application.publisher.OrderEventPublisher;
import br.com.accenture.order.application.service.OrderService;
import br.com.accenture.order.domain.enums.OrderStatus;
import br.com.accenture.order.domain.model.DeliveryAddress;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.infrastructure.messaging.dto.PaymentApprovedEvent;
import br.com.accenture.order.infrastructure.messaging.dto.PaymentCanceledEvent;
import br.com.accenture.order.infrastructure.messaging.dto.PaymentRefundedEvent;
import br.com.accenture.order.infrastructure.messaging.dto.PaymentRefusedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderEventListener listener;

    private static DeliveryAddress sampleAddress() {
        return new DeliveryAddress("Rua A", "10", null, "Bairro", "Cidade", "SP", "01001000");
    }

    private static Order orderWithStatus(UUID id, OrderStatus status) {
        return Order.restore(id, UUID.randomUUID(), status, BigDecimal.TEN, List.of(),
                sampleAddress(), Instant.now(), Instant.now(), 0L);
    }

    private static PaymentApprovedEvent paymentApproved(UUID orderId) {
        return new PaymentApprovedEvent(UUID.randomUUID(), UUID.randomUUID(), orderId,
                UUID.randomUUID(), BigDecimal.TEN, "PIX", Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("handlePaymentApproved deve marcar pedido como pago quando status nao for CANCELED")
    void shouldMarkOrderAsPaidWhenApproved() {
        UUID orderId = UUID.randomUUID();
        Order order = orderWithStatus(orderId, OrderStatus.RESERVED);
        when(orderService.findById(orderId)).thenReturn(order);

        listener.handlePaymentApproved(paymentApproved(orderId));

        verify(orderService).markOrderAsPaid(orderId);
        verify(eventPublisher, never()).publishOrderCanceledEvent(any(), any());
    }

    @Test
    @DisplayName("handlePaymentApproved deve solicitar estorno quando pedido ja esta CANCELED")
    void shouldRequestRefundWhenOrderAlreadyCanceled() {
        UUID orderId = UUID.randomUUID();
        Order canceledOrder = orderWithStatus(orderId, OrderStatus.CANCELED);
        when(orderService.findById(orderId)).thenReturn(canceledOrder);

        listener.handlePaymentApproved(paymentApproved(orderId));

        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventPublisher).publishOrderCanceledEvent(eq(canceledOrder), reasonCaptor.capture());
        assertThat(reasonCaptor.getValue()).contains("Pagamento recebido apos cancelamento");
        verify(orderService, never()).markOrderAsPaid(any());
    }

    @Test
    @DisplayName("handlePaymentApproved deve relancar IllegalStateException quando markOrderAsPaid falhar")
    void shouldRethrowIllegalStateExceptionWhenMarkAsPaidFails() {
        UUID orderId = UUID.randomUUID();
        Order order = orderWithStatus(orderId, OrderStatus.PENDING);
        when(orderService.findById(orderId)).thenReturn(order);
        doThrow(new IllegalStateException("aguardando reserva"))
                .when(orderService).markOrderAsPaid(orderId);

        assertThatThrownBy(() -> listener.handlePaymentApproved(paymentApproved(orderId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aguardando reserva");
    }

    @Test
    @DisplayName("handlePaymentRefused deve cancelar o pedido com o motivo da recusa")
    void shouldCancelOrderWhenPaymentRefused() {
        UUID orderId = UUID.randomUUID();
        PaymentRefusedEvent event = new PaymentRefusedEvent(UUID.randomUUID(), UUID.randomUUID(),
                orderId, UUID.randomUUID(), BigDecimal.TEN, "PIX", "Saldo insuficiente", Instant.now());

        listener.handlePaymentRefused(event);

        verify(orderService).cancelOrder(eq(orderId), contains("Payment refused"));
        verify(orderService).cancelOrder(eq(orderId), contains("Saldo insuficiente"));
    }

    @Test
    @DisplayName("handlePaymentCanceled deve cancelar o pedido com motivo do cancelamento")
    void shouldCancelOrderWhenPaymentCanceled() {
        UUID orderId = UUID.randomUUID();
        PaymentCanceledEvent event = new PaymentCanceledEvent(UUID.randomUUID(), UUID.randomUUID(),
                orderId, UUID.randomUUID(), BigDecimal.TEN, "PIX", "Cliente desistiu", Instant.now());

        listener.handlePaymentCanceled(event);

        verify(orderService).cancelOrder(eq(orderId), contains("Payment canceled"));
        verify(orderService).cancelOrder(eq(orderId), contains("Cliente desistiu"));
    }

    @Test
    @DisplayName("handlePaymentRefunded deve solicitar refund com o motivo do estorno")
    void shouldRefundOrderWhenPaymentRefunded() {
        UUID orderId = UUID.randomUUID();
        PaymentRefundedEvent event = new PaymentRefundedEvent(UUID.randomUUID(), UUID.randomUUID(),
                orderId, UUID.randomUUID(), BigDecimal.TEN, "PIX", "Produto avariado", Instant.now());

        listener.handlePaymentRefunded(event);

        verify(orderService).refundOrder(eq(orderId), contains("Payment refunded"));
        verify(orderService).refundOrder(eq(orderId), contains("Produto avariado"));
    }
}
