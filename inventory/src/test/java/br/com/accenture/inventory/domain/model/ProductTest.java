package br.com.accenture.inventory.domain.model;

import br.com.accenture.inventory.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ProductTest {

    @Test
    void createNewBuildsValidProduct() {
        Product product = Product.createNew("SKU-001", "Notebook", "Sample description", "Electronics", BigDecimal.TEN, 5, null);

        assertThat(product.getId()).isNull();
        assertThat(product.getSku()).isEqualTo("SKU-001");
        assertThat(product.getName()).isEqualTo("Notebook");
        assertThat(product.getDescription()).isEqualTo("Sample description");
        assertThat(product.getCategory()).isEqualTo("Electronics");
        assertThat(product.getBasePrice()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(product.getStockQuantity()).isEqualTo(5);
        assertThat(product.getVersion()).isNull();
    }

    @Test
    void createNewRejectsInvalidValues() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Product.createNew(" ", "Notebook", "Sample description", "Electronics", BigDecimal.TEN, 5, null))
                .withMessage("sku must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Product.createNew("SKU-001", "", "Sample description", "Electronics", BigDecimal.TEN, 5, null))
                .withMessage("name must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Product.createNew("SKU-001", "Notebook", " ", "Electronics", BigDecimal.TEN, 5, null))
                .withMessage("description must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Product.createNew("SKU-001", "Notebook", "Sample description", null, BigDecimal.TEN, 5, null))
                .withMessage("category must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Product.createNew("SKU-001", "Notebook", "Sample description", "Electronics", null, 5, null))
                .withMessage("basePrice must be positive");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Product.createNew("SKU-001", "Notebook", "Sample description", "Electronics", BigDecimal.ZERO, 5, null))
                .withMessage("basePrice must be positive");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Product.createNew("SKU-001", "Notebook", "Sample description", "Electronics", BigDecimal.TEN, null, null))
                .withMessage("stockQuantity must be positive or zero");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Product.createNew("SKU-001", "Notebook", "Sample description", "Electronics", BigDecimal.TEN, -1, null))
                .withMessage("stockQuantity must be positive or zero");
    }

    @Test
    void updateChangesEditableFieldsAndKeepsSku() {
        Product product = Product.createNew("SKU-001", "Notebook", "Sample description", "Electronics", BigDecimal.TEN, 5, null);

        product.update("Mouse", "Updated description", "Accessories", BigDecimal.valueOf(50), 12, "https://cdn.example.com/img.jpg");

        assertThat(product.getSku()).isEqualTo("SKU-001");
        assertThat(product.getName()).isEqualTo("Mouse");
        assertThat(product.getDescription()).isEqualTo("Updated description");
        assertThat(product.getCategory()).isEqualTo("Accessories");
        assertThat(product.getBasePrice()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(product.getStockQuantity()).isEqualTo(12);
        assertThat(product.getImageUrl()).isEqualTo("https://cdn.example.com/img.jpg");
    }

    @Test
    void stockCanBeDecreasedAndIncreased() {
        Product product = Product.createNew("SKU-001", "Notebook", "Sample description", "Electronics", BigDecimal.TEN, 5, null);

        product.decreaseStock(2);
        product.increaseStock(4);

        assertThat(product.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void decreaseStockRejectsInvalidOrUnavailableQuantity() {
        Product product = Product.createNew("SKU-001", "Notebook", "Sample description", "Electronics", BigDecimal.TEN, 5, null);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> product.decreaseStock(null))
                .withMessage("quantity must be positive");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> product.decreaseStock(0))
                .withMessage("quantity must be positive");
        assertThatExceptionOfType(InsufficientStockException.class)
                .isThrownBy(() -> product.decreaseStock(6))
                .withMessage("Insufficient stock for product sku: SKU-001. Requested: 6, available: 5");
    }

    @Test
    void increaseStockRejectsInvalidQuantity() {
        Product product = Product.createNew("SKU-001", "Notebook", "Sample description", "Electronics", BigDecimal.TEN, 5, null);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> product.increaseStock(null))
                .withMessage("quantity must be positive");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> product.increaseStock(0))
                .withMessage("quantity must be positive");
    }
}
