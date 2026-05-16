package br.com.accenture.order.infrastructure.persistence.mapper;

import br.com.accenture.order.domain.model.DeliveryAddress;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.domain.model.OrderItem;
import br.com.accenture.order.infrastructure.persistence.entity.DeliveryAddressEmbeddable;
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
                .deliveryAddress(toEmbeddable(order.getDeliveryAddress()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .version(order.getVersion())
                .build();

        var itemEntities = order.getItems().stream()
                .map(item -> OrderItemJpaEntity.builder()
                        .id(item.getId())
                        .order(orderEntity)
                        .sku(item.getSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .createdAt(item.getCreatedAt())
                        .updatedAt(item.getUpdatedAt())
                        .build())
                .toList();

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
                toDomain(entity.getDeliveryAddress()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    private static DeliveryAddressEmbeddable toEmbeddable(DeliveryAddress address) {
        if (address == null) {
            return null;
        }
        return DeliveryAddressEmbeddable.builder()
                .street(address.street())
                .number(address.number())
                .complement(address.complement())
                .neighborhood(address.neighborhood())
                .city(address.city())
                .state(address.state())
                .zipCode(address.zipCode())
                .build();
    }

    private static DeliveryAddress toDomain(DeliveryAddressEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }
        return new DeliveryAddress(
                embeddable.getStreet(),
                embeddable.getNumber(),
                embeddable.getComplement(),
                embeddable.getNeighborhood(),
                embeddable.getCity(),
                embeddable.getState(),
                embeddable.getZipCode()
        );
    }
}
