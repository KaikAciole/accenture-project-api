package br.com.accenture.order.infrastructure.messaging;

import br.com.accenture.order.application.dto.event.OrderCanceledEvent;
import br.com.accenture.order.application.dto.event.OrderCreatedEvent;
import br.com.accenture.order.application.dto.event.OrderPaidEvent;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.domain.model.OrderItem;
import br.com.accenture.order.infrastructure.config.RabbitMQConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMQOrderEventPublisherAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQOrderEventPublisherAdapter adapter;

    @Test
    @DisplayName("Deve publicar OrderCreatedEvent com o payload e routing key corretos")
    void shouldPublishOrderCreatedEvent() {
        Order order = Order.createNew("customer-123");
        order.addItem(OrderItem.createNew("SKU-99", 2, new BigDecimal("50.00")));

        adapter.publishOrderCreatedEvent(order);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq("order.created"),
                eventCaptor.capture()
        );

        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.customerId()).isEqualTo("customer-123");
        assertThat(capturedEvent.totalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(capturedEvent.items()).hasSize(1);
        assertThat(capturedEvent.items().get(0).sku()).isEqualTo("SKU-99");
    }

    @Test
    @DisplayName("Deve publicar OrderPaidEvent com o payload e routing key corretos")
    void shouldPublishOrderPaidEvent() {
        Order order = Order.restore(UUID.randomUUID(), "customer-123", null, BigDecimal.TEN, null, null, null);

        adapter.publishOrderPaidEvent(order);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq("order.paid"),
                any(OrderPaidEvent.class)
        );
    }

    @Test
    @DisplayName("Deve publicar OrderCanceledEvent com o motivo e routing key corretos")
    void shouldPublishOrderCanceledEvent() {
        Order order = Order.restore(UUID.randomUUID(), "customer-123", null, BigDecimal.TEN, null, null, null);
        String reason = "Pagamento Recusado";

        adapter.publishOrderCanceledEvent(order, reason);

        ArgumentCaptor<OrderCanceledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCanceledEvent.class);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq("order.canceled"),
                eventCaptor.capture()
        );

        assertThat(eventCaptor.getValue().reason()).isEqualTo(reason);
    }
}