package br.com.accenture.order.infrastructure.persistence.mapper;

import br.com.accenture.order.domain.enums.OrderStatus;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.domain.model.OrderItem;
import br.com.accenture.order.infrastructure.persistence.entity.OrderJpaEntity;
import br.com.accenture.order.infrastructure.persistence.entity.OrderItemJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPersistenceMapperTest {

    @Test
    @DisplayName("Deve converter Domain para Entity corretamente")
    void shouldMapDomainToEntity() {
        Order domainOrder = Order.createNew("customer-123");
        domainOrder.addItem(OrderItem.createNew("SKU-99", 2, new BigDecimal("50.00")));

        OrderJpaEntity entity = OrderPersistenceMapper.toEntity(domainOrder);

        assertThat(entity).isNotNull();
        assertThat(entity.getCustomerId()).isEqualTo("customer-123");
        assertThat(entity.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(entity.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(entity.getItems()).hasSize(1);

        OrderItemJpaEntity itemEntity = entity.getItems().get(0);
        assertThat(itemEntity.getSku()).isEqualTo("SKU-99");
        assertThat(itemEntity.getQuantity()).isEqualTo(2);
        assertThat(itemEntity.getUnitPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(itemEntity.getOrder()).isEqualTo(entity);
    }

    @Test
    @DisplayName("Deve converter Entity para Domain corretamente")
    void shouldMapEntityToDomain() {
        UUID orderId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Instant now = Instant.now();

        OrderJpaEntity entity = OrderJpaEntity.builder()
                .id(orderId)
                .customerId("customer-123")
                .status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("200.00"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        OrderItemJpaEntity itemEntity = OrderItemJpaEntity.builder()
                .id(itemId)
                .order(entity)
                .sku("SKU-88")
                .quantity(4)
                .unitPrice(new BigDecimal("50.00"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        entity.setItems(List.of(itemEntity));

        Order domainOrder = OrderPersistenceMapper.toDomain(entity);

        assertThat(domainOrder).isNotNull();
        assertThat(domainOrder.getId()).isEqualTo(orderId);
        assertThat(domainOrder.getCustomerId()).isEqualTo("customer-123");
        assertThat(domainOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(domainOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(domainOrder.getCreatedAt()).isEqualTo(now);
        assertThat(domainOrder.getUpdatedAt()).isEqualTo(now);

        assertThat(domainOrder.getItems()).hasSize(1);
        OrderItem domainItem = domainOrder.getItems().get(0);
        assertThat(domainItem.getId()).isEqualTo(itemId);
        assertThat(domainItem.getSku()).isEqualTo("SKU-88");
        assertThat(domainItem.getQuantity()).isEqualTo(4);
    }

    @Test
    @DisplayName("Deve retornar nulo quando Domain for nulo")
    void shouldReturnNullWhenDomainIsNull() {
        assertThat(OrderPersistenceMapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("Deve retornar nulo quando Entity for nula")
    void shouldReturnNullWhenEntityIsNull() {
        assertThat(OrderPersistenceMapper.toDomain(null)).isNull();
    }
}