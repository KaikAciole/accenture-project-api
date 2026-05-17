package br.com.accenture.order.infrastructure.messaging.listener;

import br.com.accenture.order.application.service.OrderService;
import br.com.accenture.order.infrastructure.messaging.dto.StockEvents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockEventListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private StockEventListener listener;

    @Test
    @DisplayName("handleStockReserved deve chamar markOrderAsReserved com o orderId do evento")
    void shouldMarkOrderAsReservedOnStockReservedEvent() {
        UUID orderId = UUID.randomUUID();

        listener.handleStockReserved(new StockEvents.Reserved(orderId));

        verify(orderService).markOrderAsReserved(orderId);
    }

    @Test
    @DisplayName("handleStockFailed deve cancelar o pedido com a razao do evento")
    void shouldCancelOrderOnStockFailedEvent() {
        UUID orderId = UUID.randomUUID();
        String reason = "SKU-1 indisponivel";

        listener.handleStockFailed(new StockEvents.Failed(orderId, reason));

        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderService).cancelOrder(eq(orderId), reasonCaptor.capture());
        assertThat(reasonCaptor.getValue())
                .startsWith("Stock reservation failed:")
                .contains(reason);
    }

    @Test
    @DisplayName("handleStockFailed deve propagar excecao quando o servico falhar")
    void shouldPropagateExceptionWhenServiceFails() {
        UUID orderId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(orderService).cancelOrder(eq(orderId), contains("Stock reservation failed"));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> listener.handleStockFailed(new StockEvents.Failed(orderId, "qualquer"))
        ).isInstanceOf(RuntimeException.class);
    }
}
