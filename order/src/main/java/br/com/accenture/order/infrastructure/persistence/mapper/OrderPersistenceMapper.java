package br.com.accenture.order.infrastructure.persistence.mapper;

import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.domain.model.OrderItem;
import br.com.accenture.order.infrastructure.persistence.entity.OrderJpaEntity;
import br.com.accenture.order.infrastructure.persistence.entity.OrderItemJpaEntity;

import java.util.ArrayList;

public final class OrderPersistenceMapper {

    private OrderPersistenceMapper() {
    }

    public static OrderJpaEntity toEntity(Order order) {
        if (order == null) {
            return null;
        }

        var orderEntity = OrderJpaEntity.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();

        var itemEntities = order.getItems().stream()
                .map(item -> OrderItemJpaEntity.builder()
                        .id(item.getId())
                        .order(orderEntity) // Vinculando a chave estrangeira aqui!
                        .sku(item.getSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .createdAt(item.getCreatedAt())
                        .updatedAt(item.getUpdatedAt())
                        .build())
                .toList();

        // O Spring Data precisa que a lista seja mutável para gerenciar o estado
        orderEntity.setItems(new ArrayList<>(itemEntities));

        return orderEntity;
    }

    public static Order toDomain(OrderJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        var items = entity.getItems().stream()
                .map(itemEntity -> OrderItem.restore(
                        itemEntity.getId(),
                        itemEntity.getSku(),
                        itemEntity.getQuantity(),
                        itemEntity.getUnitPrice(),
                        itemEntity.getCreatedAt(),
                        itemEntity.getUpdatedAt()
                ))
                .toList();

        return Order.restore(
                entity.getId(),
                entity.getCustomerId(),
                entity.getStatus(),
                entity.getTotalAmount(),
                new ArrayList<>(items),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}