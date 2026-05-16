package br.com.accenture.order.domain.model;

import br.com.accenture.order.domain.enums.OrderStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Order {

    private UUID id;
    private UUID customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItem> items;
    private DeliveryAddress deliveryAddress;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    private Order(UUID id,
                  UUID customerId,
                  OrderStatus status,
                  BigDecimal totalAmount,
                  List<OrderItem> items,
                  DeliveryAddress deliveryAddress,
                  Instant createdAt,
                  Instant updatedAt,
                  Long version) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.items = items != null ? items : new ArrayList<>();
        this.deliveryAddress = deliveryAddress;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Order createNew(UUID customerId, DeliveryAddress deliveryAddress) {
        requireNotNull(customerId, "customerId");
        requireNotNull(deliveryAddress, "deliveryAddress");
        Instant now = Instant.now();
        return new Order(null, customerId, OrderStatus.PENDING, BigDecimal.ZERO, new ArrayList<>(), deliveryAddress, now, now, null);
    }

    public static Order restore(UUID id,
                                UUID customerId,
                                OrderStatus status,
                                BigDecimal totalAmount,
                                List<OrderItem> items,
                                DeliveryAddress deliveryAddress,
                                Instant createdAt,
                                Instant updatedAt,
                                Long version) {
        return new Order(id, customerId, status, totalAmount, items, deliveryAddress, createdAt, updatedAt, version);
    }

    public void addItem(OrderItem item) {
        requireNotNull(item, "item");
        this.items.add(item);
        recalculateTotalAmount();
        this.updatedAt = Instant.now();
    }

    public void removeItem(OrderItem item) {
        requireNotNull(item, "item");
        this.items.remove(item);
        recalculateTotalAmount();
        this.updatedAt = Instant.now();
    }

    public void markAsPaid() {
        if (this.status == OrderStatus.PAID) return;

        if (this.status != OrderStatus.RESERVED) {
            throw new IllegalStateException("Cannot mark as paid from current state: " + this.status + ". Order must be RESERVED first.");
        }

        this.status = OrderStatus.PAID;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (this.status == OrderStatus.CANCELED) return;
        if (this.status == OrderStatus.PAID || this.status == OrderStatus.REFUNDED) {
            throw new IllegalStateException("Cannot cancel an already paid or refunded order. Use refund.");
        }
        this.status = OrderStatus.CANCELED;
        this.updatedAt = Instant.now();
    }

    public void markAsReserved() {
        if (this.status == OrderStatus.RESERVED) return;
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot reserve order from current state: " + this.status);
        }
        this.status = OrderStatus.RESERVED;
        this.updatedAt = Instant.now();
    }

    public void markAsRefunded() {
        if (this.status == OrderStatus.REFUNDED) return;
        if (this.status != OrderStatus.PAID) {
            throw new IllegalStateException("Cannot refund an order that is not paid.");
        }
        this.status = OrderStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    private void recalculateTotalAmount() {
        this.totalAmount = this.items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void requireNotNull(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
    }
}