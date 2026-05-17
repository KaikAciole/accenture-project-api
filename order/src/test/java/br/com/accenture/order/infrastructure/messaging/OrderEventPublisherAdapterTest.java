package br.com.accenture.order.infrastructure.messaging;

import br.com.accenture.order.domain.enums.OrderStatus;
import br.com.accenture.order.domain.model.DeliveryAddress;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.domain.model.OrderItem;
import br.com.accenture.order.infrastructure.persistence.OutboxEventRepository;
import br.com.accenture.order.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherAdapterTest {

    @Mock
    private OutboxEventRepository outboxRepository;

    private ObjectMapper objectMapper;
    private OrderEventPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        adapter = new OrderEventPublisherAdapter(outboxRepository, objectMapper);
    }

    private static DeliveryAddress sampleAddress() {
        return new DeliveryAddress("Rua A", "10", "Apto 1", "Bairro", "Cidade", "SP", "01001000");
    }

    private static Order sampleOrder() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderItem item = OrderItem.restore(UUID.randomUUID(), "SKU-1", 2, new BigDecimal("10.00"), Instant.now(), Instant.now());
        return Order.restore(orderId, customerId, OrderStatus.PENDING, new BigDecimal("20.00"),
                List.of(item), sampleAddress(), Instant.now(), Instant.now(), 0L);
    }

    @Test
    @DisplayName("Deve gravar OrderCreatedEvent no outbox com aggregateType e eventType corretos")
    void shouldSaveOrderCreatedEventToOutbox() {
        Order order = sampleOrder();

        adapter.publishOrderCreatedEvent(order);

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEventJpaEntity saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("Order");
        assertThat(saved.getAggregateId()).isEqualTo(order.getId().toString());
        assertThat(saved.getEventType()).isEqualTo("order.created");
        assertThat(saved.isProcessed()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getPayload()).contains(order.getId().toString());
        assertThat(saved.getPayload()).contains("SKU-1");
    }

    @Test
    @DisplayName("Deve gravar OrderPaidEvent no outbox")
    void shouldSaveOrderPaidEventToOutbox() {
        Order order = sampleOrder();

        adapter.publishOrderPaidEvent(order);

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEventJpaEntity saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("order.paid");
        assertThat(saved.getAggregateId()).isEqualTo(order.getId().toString());
    }

    @Test
    @DisplayName("Deve gravar OrderReservedEvent no outbox")
    void shouldSaveOrderReservedEventToOutbox() {
        Order order = sampleOrder();

        adapter.publishOrderReservedEvent(order);

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxRepository).save(captor.capture());

        assertThat(captor.getValue().getEventType()).isEqualTo("order.reserved");
    }

    @Test
    @DisplayName("Deve gravar OrderCanceledEvent no outbox incluindo motivo no payload")
    void shouldSaveOrderCanceledEventToOutbox() {
        Order order = sampleOrder();
        String reason = "Estoque insuficiente";

        adapter.publishOrderCanceledEvent(order, reason);

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEventJpaEntity saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("order.canceled");
        assertThat(saved.getPayload()).contains(reason);
    }

    @Test
    @DisplayName("Deve gravar OrderRefundedEvent no outbox incluindo motivo no payload")
    void shouldSaveOrderRefundedEventToOutbox() {
        Order order = sampleOrder();
        String reason = "Pagamento estornado";

        adapter.publishOrderRefundedEvent(order, reason);

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEventJpaEntity saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("order.refunded");
        assertThat(saved.getPayload()).contains(reason);
    }

    @Test
    @DisplayName("Deve lancar RuntimeException quando a serializacao do payload falhar")
    void shouldThrowRuntimeExceptionWhenSerializationFails() throws JsonProcessingException {
        ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});

        OrderEventPublisherAdapter failingAdapter = new OrderEventPublisherAdapter(outboxRepository, failingMapper);

        assertThatThrownBy(() -> failingAdapter.publishOrderPaidEvent(sampleOrder()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to serialize event payload")
                .hasCauseInstanceOf(JsonProcessingException.class);
    }
}
