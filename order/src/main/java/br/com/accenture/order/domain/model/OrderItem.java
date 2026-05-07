package br.com.accenture.order.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class OrderItem {

    private UUID id;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Instant createdAt;
    private Instant updatedAt;

    private OrderItem(UUID id,
                      String sku,
                      Integer quantity,
                      BigDecimal unitPrice,
                      Instant createdAt,
                      Instant updatedAt) {
        this.id = id;
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OrderItem createNew(String sku, Integer quantity, BigDecimal unitPrice) {
        requireNotBlank(sku, "sku");
        requireNotNull(quantity, "quantity");
        requireNotNull(unitPrice, "unitPrice");
        requirePositive(quantity, "quantity");
        requirePositiveOrZero(unitPrice, "unitPrice");

        return new OrderItem(null, sku, quantity, unitPrice, null, null);
    }

    public static OrderItem restore(UUID id,
                                    String sku,
                                    Integer quantity,
                                    BigDecimal unitPrice,
                                    Instant createdAt,
                                    Instant updatedAt) {
        return new OrderItem(id, sku, quantity, unitPrice, createdAt, updatedAt);
    }

    public void updateQuantity(Integer newQuantity) {
        requireNotNull(newQuantity, "newQuantity");
        requirePositive(newQuantity, "newQuantity");
        this.quantity = newQuantity;
    }

    public BigDecimal getTotalPrice() {
        return this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
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

    private static void requirePositive(Integer value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }

    private static void requirePositiveOrZero(BigDecimal value, String field) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " must be zero or greater");
        }
    }
}