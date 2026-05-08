package br.com.accenture.order.domain.model;

import br.com.accenture.order.domain.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    @DisplayName("Deve inicializar um pedido corretamente para um cliente")
    void shouldInitializeOrderCorrectly() {
        Order order = Order.createNew("customer-99");

        assertThat(order.getCustomerId()).isEqualTo("customer-99");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Deve lancar excecao ao criar pedido sem customerId")
    void shouldThrowExceptionWhenCustomerIdIsInvalid() {
        assertThatThrownBy(() -> Order.createNew(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Order.createNew(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve adicionar itens e recalcular o valor total do pedido")
    void shouldAddItemsAndRecalculateTotalAmount() {
        Order order = Order.createNew("customer-99");
        OrderItem item1 = OrderItem.createNew("SKU-A", 2, new BigDecimal("100.00"));
        OrderItem item2 = OrderItem.createNew("SKU-B", 1, new BigDecimal("50.00"));

        order.addItem(item1);
        order.addItem(item2);

        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    @DisplayName("Deve lancar excecao ao tentar adicionar item nulo")
    void shouldThrowExceptionWhenAddingNullItem() {
        Order order = Order.createNew("customer-99");

        assertThatThrownBy(() -> order.addItem(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve remover item e recalcular o valor total do pedido")
    void shouldRemoveItemAndRecalculateTotalAmount() {
        Order order = Order.createNew("customer-99");
        OrderItem item1 = OrderItem.createNew("SKU-A", 2, new BigDecimal("100.00"));
        OrderItem item2 = OrderItem.createNew("SKU-B", 1, new BigDecimal("50.00"));

        order.addItem(item1);
        order.addItem(item2);
        order.removeItem(item1);

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Deve lancar excecao ao tentar remover item nulo")
    void shouldThrowExceptionWhenRemovingNullItem() {
        Order order = Order.createNew("customer-99");

        assertThatThrownBy(() -> order.removeItem(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve permitir a reserva do pedido se estiver no status correto")
    void shouldConfirmReservationWhenStatusIsPending() {
        Order order = Order.createNew("customer-99");

        order.confirmReservation();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RESERVED);
    }

    @Test
    @DisplayName("Deve bloquear a reserva do pedido se estiver em um status invalido")
    void shouldThrowExceptionWhenReservingInvalidOrderState() {
        Order order = Order.createNew("customer-99");
        order.updateStatus(OrderStatus.CANCELED);

        assertThatThrownBy(order::confirmReservation)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Deve lancar excecao ao atualizar para status nulo")
    void shouldThrowExceptionWhenUpdatingToNullStatus() {
        Order order = Order.createNew("customer-99");

        assertThatThrownBy(() -> order.updateStatus(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}