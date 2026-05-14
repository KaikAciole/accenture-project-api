package br.com.accenture.order.infrastructure.messaging;

import br.com.accenture.order.application.dto.event.OrderCanceledEvent;
import br.com.accenture.order.application.dto.event.OrderCreatedEvent;
import br.com.accenture.order.application.dto.event.OrderPaidEvent;
import br.com.accenture.order.application.publisher.OrderEventPublisher;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.infrastructure.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQOrderEventPublisherAdapter implements OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQOrderEventPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
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

        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, "order.created", event);
    }

    @Override
    public void publishOrderPaidEvent(Order order) {
        var event = new OrderPaidEvent(
                order.getId(),
                order.getCustomerId()
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, "order.paid", event);
    }

    @Override
    public void publishOrderCanceledEvent(Order order, String reason) {
        var event = new OrderCanceledEvent(
                order.getId(),
                order.getCustomerId(),
                reason
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, "order.canceled", event);
    }
}