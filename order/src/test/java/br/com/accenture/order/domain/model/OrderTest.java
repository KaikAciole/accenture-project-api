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
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
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
    @DisplayName("Deve marcar o pedido como pago se estiver pendente")
    void shouldMarkAsPaidWhenStatusIsPending() {
        Order order = Order.createNew("customer-99");

        order.markAsPaid();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("Deve bloquear a marcacao como pago se o pedido ja estiver cancelado")
    void shouldThrowExceptionWhenMarkingAsPaidFromInvalidState() {
        Order order = Order.createNew("customer-99");
        order.cancel();

        assertThatThrownBy(order::markAsPaid)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot mark as paid from current state");
    }

    @Test
    @DisplayName("Deve cancelar o pedido com sucesso se nao estiver pago")
    void shouldCancelOrderSuccessfully() {
        Order order = Order.createNew("customer-99");

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("Deve bloquear o cancelamento se o pedido ja estiver pago")
    void shouldThrowExceptionWhenCancelingAlreadyPaidOrder() {
        Order order = Order.createNew("customer-99");
        order.markAsPaid();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel an already paid order");
    }
}