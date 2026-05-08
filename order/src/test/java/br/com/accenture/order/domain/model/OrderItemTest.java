package br.com.accenture.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemTest {

    @Test
    @DisplayName("Deve criar um item de pedido valido e calcular o total corretamente")
    void shouldCreateValidOrderItemAndCalculateTotal() {
        OrderItem item = OrderItem.createNew("SKU-123", 3, new BigDecimal("50.00"));

        assertThat(item.getSku()).isEqualTo("SKU-123");
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getUnitPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(item.getTotalPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Deve lancar excecao ao tentar criar item com quantidade invalida")
    void shouldThrowExceptionWhenQuantityIsInvalid() {
        assertThatThrownBy(() -> OrderItem.createNew("SKU-123", 0, new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> OrderItem.createNew("SKU-123", -1, new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> OrderItem.createNew("SKU-123", null, new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve lancar excecao ao tentar criar item com SKU invalido")
    void shouldThrowExceptionWhenSkuIsInvalid() {
        assertThatThrownBy(() -> OrderItem.createNew("", 1, new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> OrderItem.createNew(null, 1, new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve lancar excecao ao tentar criar item com preco unitario invalido")
    void shouldThrowExceptionWhenUnitPriceIsInvalid() {
        assertThatThrownBy(() -> OrderItem.createNew("SKU-123", 1, new BigDecimal("-10.00")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> OrderItem.createNew("SKU-123", 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve atualizar a quantidade corretamente")
    void shouldUpdateQuantity() {
        OrderItem item = OrderItem.createNew("SKU-123", 2, new BigDecimal("50.00"));

        item.updateQuantity(5);

        assertThat(item.getQuantity()).isEqualTo(5);
        assertThat(item.getTotalPrice()).isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    @DisplayName("Deve lancar excecao ao atualizar para quantidade invalida")
    void shouldThrowExceptionWhenUpdatingToInvalidQuantity() {
        OrderItem item = OrderItem.createNew("SKU-123", 2, new BigDecimal("50.00"));

        assertThatThrownBy(() -> item.updateQuantity(0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> item.updateQuantity(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}