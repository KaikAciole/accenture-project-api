package br.com.accenture.order.domain.model;


import br.com.accenture.order.domain.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "orderId")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    @NotBlank
    @Column(name = "customer_id", nullable = false, updatable = false)
    private String customerId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @NotNull
    @PositiveOrZero
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    private Order(String customerId) {
        this.customerId = customerId;
        this.orderStatus = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO;
    }

    private Order(UUID orderId, String customerId, OrderStatus orderStatus, BigDecimal totalAmount, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderStatus = orderStatus;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    public static Order initiateForCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be empty");
        }
        return new Order(customerId);
    }

    public static Order restore(UUID orderId, String customerId, OrderStatus orderStatus, BigDecimal totalAmount, LocalDateTime createdAt) {
        return new Order(orderId, customerId, orderStatus, totalAmount, createdAt);
    }

    public void addItem(OrderItem item) {
        this.items.add(item);
        item.linkToOrder(this);
        recalculateTotal();
    }

    public void removeItem(OrderItem item) {
        this.items.remove(item);
        item.linkToOrder(null);
        recalculateTotal();
    }

    private void recalculateTotal() {
        this.totalAmount = this.items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void confirmReservation() {
        if (this.orderStatus != OrderStatus.PENDING && this.orderStatus != OrderStatus.ANALYZING_FRAUD) {
            throw new IllegalStateException("Invalid state transition to RESERVED");
        }
        this.orderStatus = OrderStatus.RESERVED;
    }
}