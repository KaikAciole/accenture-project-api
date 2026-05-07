package br.com.accenture.order.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tb_order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "orderItemId")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderItemId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotBlank
    @Column(name = "sku", nullable = false, updatable = false)
    private String sku;

    @NotNull
    @Positive
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @PositiveOrZero
    @Column(name = "unit_price", nullable = false, updatable = false)
    private BigDecimal unitPrice;

    private OrderItem(String sku, Integer quantity, BigDecimal unitPrice) {
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    private OrderItem(UUID orderItemId, Order order, String sku, Integer quantity, BigDecimal unitPrice) {
        this.orderItemId = orderItemId;
        this.order = order;
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public static OrderItem create(String sku, Integer quantity, BigDecimal unitPrice) {
        if (sku == null || sku.isBlank() || quantity == null || quantity <= 0 || unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid OrderItem parameters");
        }
        return new OrderItem(sku, quantity, unitPrice);
    }

    public static OrderItem restore(UUID orderItemId, Order order, String sku, Integer quantity, BigDecimal unitPrice) {
        return new OrderItem(orderItemId, order, sku, quantity, unitPrice);
    }

    void linkToOrder(Order order) {
        this.order = order;
    }

    public BigDecimal getTotalPrice() {
        return this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
    }
}