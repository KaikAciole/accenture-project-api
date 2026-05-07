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
    private String customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItem> items;
    private Instant createdAt;
    private Instant updatedAt;

    private Order(UUID id,
                  String customerId,
                  OrderStatus status,
                  BigDecimal totalAmount,
                  List<OrderItem> items,
                  Instant createdAt,
                  Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.items = items != null ? items : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order createNew(String customerId) {
        requireNotBlank(customerId, "customerId");
        return new Order(null, customerId, OrderStatus.PENDING, BigDecimal.ZERO, new ArrayList<>(), null, null);
    }

    public static Order restore(UUID id,
                                String customerId,
                                OrderStatus status,
                                BigDecimal totalAmount,
                                List<OrderItem> items,
                                Instant createdAt,
                                Instant updatedAt) {
        return new Order(id, customerId, status, totalAmount, items, createdAt, updatedAt);
    }

    public void addItem(OrderItem item) {
        requireNotNull(item, "item");
        this.items.add(item);
        recalculateTotalAmount();
    }

    public void removeItem(OrderItem item) {
        requireNotNull(item, "item");
        this.items.remove(item);
        recalculateTotalAmount();
    }

    public void updateStatus(OrderStatus newStatus) {
        requireNotNull(newStatus, "newStatus");
        // Aqui entrariam validações de máquina de estado (ex: não pode ir de CANCELED para PAID)
        this.status = newStatus;
    }

    public void confirmReservation() {
        if (this.status != OrderStatus.PENDING && this.status != OrderStatus.ANALYZING_FRAUD) {
            throw new IllegalStateException("Cannot reserve order from current state: " + this.status);
        }
        this.status = OrderStatus.RESERVED;
    }

    private void recalculateTotalAmount() {
        this.totalAmount = this.items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private static void requireNotNull(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
    }
}