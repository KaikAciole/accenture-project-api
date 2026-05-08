package br.com.accenture.inventory.domain.model;

import br.com.accenture.inventory.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ProductTest {

    @Test
    void createNewBuildsValidProduct() {
        Product product = Product.createNew("SKU-001", "Notebook", "Electronics", BigDecimal.TEN, 5);

        assertThat(product.getId()).isNull();
        assertThat(product.getSku()).isEqualTo("SKU-001");
        assertThat(product.getName()).isEqualTo("Notebook");
        assertThat(product.getCategory()).isEqualTo("Electronics");
        assertThat(product.getBasePrice()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(product.getStockQuantity()).isEqualTo(5);
        assertThat(product.getVersion()).isNull();
    }

    @Test
    void createNewRejectsInvalidValues() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Product.createNew(" ", "Notebook", "Electronics", BigDecimal.TEN, 5))
                .withMessage("sku must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Product.createNew("SKU-001", "", "Electronics", BigDecimal.TEN, 5))
                .withMessage("name must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Product.createNew("SKU-001", "Notebook", null, BigDecimal.TEN, 5))
                .withMessage("category must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Product.createNew("SKU-001", "Notebook", "Electronics", BigDecimal.ZERO, 5))
                .withMessage("basePrice must be positive");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Product.createNew("SKU-001", "Notebook", "Electronics", BigDecimal.TEN, -1))
                .withMessage("stockQuantity must be positive or zero");
    }

    @Test
    void updateChangesEditableFieldsAndKeepsSku() {
        Product product = Product.createNew("SKU-001", "Notebook", "Electronics", BigDecimal.TEN, 5);

        product.update("Mouse", "Accessories", BigDecimal.valueOf(50), 12);

        assertThat(product.getSku()).isEqualTo("SKU-001");
        assertThat(product.getName()).isEqualTo("Mouse");
        assertThat(product.getCategory()).isEqualTo("Accessories");
        assertThat(product.getBasePrice()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(product.getStockQuantity()).isEqualTo(12);
    }

    @Test
    void stockCanBeDecreasedAndIncreased() {
        Product product = Product.createNew("SKU-001", "Notebook", "Electronics", BigDecimal.TEN, 5);

        product.decreaseStock(2);
        product.increaseStock(4);

        assertThat(product.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void decreaseStockRejectsInvalidOrUnavailableQuantity() {
        Product product = Product.createNew("SKU-001", "Notebook", "Electronics", BigDecimal.TEN, 5);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> product.decreaseStock(0))
                .withMessage("quantity must be positive");
        assertThatExceptionOfType(InsufficientStockException.class)
                .isThrownBy(() -> product.decreaseStock(6))
                .withMessage("Insufficient stock for product sku: SKU-001. Requested: 6, available: 5");
    }
}
