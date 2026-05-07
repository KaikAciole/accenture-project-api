package br.com.accenture.inventory.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "SKU is required")
    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Base price must be greater than zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be zero or greater")
    @Column(nullable = false)
    private Integer stockQuantity;

    @Version
    @Column(nullable = false)
    private Long version;

    public void decreaseStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }

        if (this.stockQuantity < quantity) {
            throw new IllegalArgumentException("Insufficient stock");
        }

        this.stockQuantity -= quantity;
    }
}