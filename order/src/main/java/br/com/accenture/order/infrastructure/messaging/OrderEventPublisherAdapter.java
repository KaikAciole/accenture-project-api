package br.com.accenture.order.infrastructure.messaging;

import br.com.accenture.order.application.dto.event.OrderCanceledEvent;
import br.com.accenture.order.application.dto.event.OrderCreatedEvent;
import br.com.accenture.order.application.dto.event.OrderPaidEvent;
import br.com.accenture.order.application.dto.event.OrderRefundedEvent;
import br.com.accenture.order.application.dto.event.OrderReservedEvent;
import br.com.accenture.order.application.publisher.OrderEventPublisher;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.infrastructure.persistence.OutboxEventRepository;
import br.com.accenture.order.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OrderEventPublisherAdapter implements OrderEventPublisher {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderEventPublisherAdapter(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishOrderCreatedEvent(Order order) {
        var items = order.getItems().stream()
                .map(item -> new OrderCreatedEvent.ItemEvent(item.getSku(), item.getQuantity()))
                .toList();

        var event = new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                items
        );

        saveToOutbox("Order", order.getId().toString(), "order.created", event);
    }

    @Override
    public void publishOrderPaidEvent(Order order) {
        var event = new OrderPaidEvent(order.getId(), order.getCustomerId());
        saveToOutbox("Order", order.getId().toString(), "order.paid", event);
    }

    @Override
    public void publishOrderReservedEvent(Order order) {
        var event = new OrderReservedEvent(order.getId(), order.getCustomerId());
        saveToOutbox("Order", order.getId().toString(), "order.reserved", event);
    }

    @Override
    public void publishOrderCanceledEvent(Order order, String reason) {
        var event = new OrderCanceledEvent(order.getId(), order.getCustomerId(), reason);
        saveToOutbox("Order", order.getId().toString(), "order.canceled", event);
    }

    @Override
    public void publishOrderRefundedEvent(Order order, String reason) {
        var event = new OrderRefundedEvent(order.getId(), order.getCustomerId(), reason);
        saveToOutbox("Order", order.getId().toString(), "order.refunded", event);
    }

    private void saveToOutbox(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);

            OutboxEventJpaEntity outboxEntity = OutboxEventJpaEntity.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(jsonPayload)
                    .createdAt(Instant.now())
                    .processed(false)
                    .build();

            outboxRepository.save(outboxEntity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload", e);
        }
    }
}