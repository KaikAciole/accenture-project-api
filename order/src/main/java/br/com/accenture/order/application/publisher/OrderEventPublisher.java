package br.com.accenture.order.application.publisher;

import br.com.accenture.order.domain.model.Order;

public interface OrderEventPublisher {
    void publishOrderCreatedEvent(Order order);
    void publishOrderPaidEvent(Order order);
    void publishOrderCanceledEvent(Order order, String reason);
    void publishOrderReservedEvent(Order order);
}
