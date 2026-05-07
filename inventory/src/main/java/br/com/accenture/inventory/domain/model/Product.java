package br.com.accenture.inventory.domain.model;

import br.com.accenture.inventory.domain.exception.InsufficientStockException;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class Product {

    private UUID id;
    private String sku;
    private String name;
    private String category;
    private BigDecimal basePrice;
    private Integer stockQuantity;
    private Long version;

    private Product(UUID id,
                    String sku,
                    String name,
                    String category,
                    BigDecimal basePrice,
                    Integer stockQuantity,
                    Long version) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.basePrice = basePrice;
        this.stockQuantity = stockQuantity;
        this.version = version;
    }

    public static Product createNew(String sku,
                                    String name,
                                    String category,
                                    BigDecimal basePrice,
                                    Integer stockQuantity) {
        requireNotBlank(sku, "sku");
        requireNotBlank(name, "name");
        requireNotBlank(category, "category");
        requirePositive(basePrice);
        requirePositiveOrZero(stockQuantity);

        return new Product(null, sku, name, category, basePrice, stockQuantity, null);
    }

    public static Product restore(UUID id,
                                  String sku,
                                  String name,
                                  String category,
                                  BigDecimal basePrice,
                                  Integer stockQuantity,
                                  Long version) {
        return new Product(id, sku, name, category, basePrice, stockQuantity, version);
    }

    public void update(String name,
                       String category,
                       BigDecimal basePrice,
                       Integer stockQuantity) {
        requireNotBlank(name, "name");
        requireNotBlank(category, "category");
        requirePositive(basePrice);
        requirePositiveOrZero(stockQuantity);

        this.name = name;
        this.category = category;
        this.basePrice = basePrice;
        this.stockQuantity = stockQuantity;
    }

    public void decreaseStock(Integer quantity) {
        requirePositive(quantity);

        if (this.stockQuantity < quantity) {
            throw new InsufficientStockException(this.sku, quantity, this.stockQuantity);
        }

        this.stockQuantity -= quantity;
    }

    public void increaseStock(Integer quantity) {
        requirePositive(quantity);

        this.stockQuantity += quantity;
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private static void requirePositive(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("basePrice must be positive");
        }
    }

    private static void requirePositive(Integer value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    private static void requirePositiveOrZero(Integer value) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("stockQuantity must be positive or zero");
        }
    }
}