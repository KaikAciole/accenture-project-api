package br.com.accenture.order.infrastructure.persistence.mapper;

import br.com.accenture.order.domain.enums.OrderStatus;
import br.com.accenture.order.domain.model.DeliveryAddress;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.domain.model.OrderItem;
import br.com.accenture.order.infrastructure.persistence.entity.DeliveryAddressEmbeddable;
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

    private static DeliveryAddress sampleAddress() {
        return new DeliveryAddress(
                "Rua das Flores", "123", "Apto 1", "Centro", "São Paulo", "SP", "01001000"
        );
    }

    @Test
    @DisplayName("Deve converter Domain para Entity corretamente")
    void shouldMapDomainToEntity() {
        UUID customerId = UUID.randomUUID();
        Order domainOrder = Order.createNew(customerId, sampleAddress());
        domainOrder.addItem(OrderItem.createNew("SKU-99", 2, new BigDecimal("50.00")));

        OrderJpaEntity entity = OrderPersistenceMapper.toEntity(domainOrder);

        assertThat(entity).isNotNull();
        assertThat(entity.getCustomerId()).isEqualTo(customerId);
        assertThat(entity.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(entity.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(entity.getItems()).hasSize(1);
        assertThat(entity.getDeliveryAddress()).isNotNull();
        assertThat(entity.getDeliveryAddress().getStreet()).isEqualTo("Rua das Flores");

        assertThat(entity.getCreatedAt()).isEqualTo(domainOrder.getCreatedAt());
        assertThat(entity.getUpdatedAt()).isEqualTo(domainOrder.getUpdatedAt());

        OrderItemJpaEntity itemEntity = entity.getItems().get(0);
        assertThat(itemEntity.getSku()).isEqualTo("SKU-99");
        assertThat(itemEntity.getQuantity()).isEqualTo(2);
        assertThat(itemEntity.getUnitPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(itemEntity.getOrder()).isEqualTo(entity);

        OrderItem domainItem = domainOrder.getItems().get(0);
        assertThat(itemEntity.getCreatedAt()).isEqualTo(domainItem.getCreatedAt());
        assertThat(itemEntity.getUpdatedAt()).isEqualTo(domainItem.getUpdatedAt());
    }

    @Test
    @DisplayName("Deve converter Entity para Domain corretamente")
    void shouldMapEntityToDomain() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Instant now = Instant.now();

        DeliveryAddressEmbeddable embeddable = DeliveryAddressEmbeddable.builder()
                .street("Rua das Flores").number("123").complement("Apto 1")
                .neighborhood("Centro").city("São Paulo").state("SP").zipCode("01001000")
                .build();

        OrderJpaEntity entity = OrderJpaEntity.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("200.00"))
                .deliveryAddress(embeddable)
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
        assertThat(domainOrder.getCustomerId()).isEqualTo(customerId);
        assertThat(domainOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(domainOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(domainOrder.getCreatedAt()).isEqualTo(now);
        assertThat(domainOrder.getUpdatedAt()).isEqualTo(now);
        assertThat(domainOrder.getDeliveryAddress()).isNotNull();
        assertThat(domainOrder.getDeliveryAddress().city()).isEqualTo("São Paulo");

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
