package br.com.accenture.order.domain.model;

import br.com.accenture.order.domain.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static DeliveryAddress sampleAddress() {
        return new DeliveryAddress(
                "Rua das Flores", "123", "Apto 1", "Centro", "São Paulo", "SP", "01001000"
        );
    }

    @Test
    @DisplayName("Deve inicializar um pedido corretamente para um cliente")
    void shouldInitializeOrderCorrectly() {
        UUID customerId = UUID.randomUUID();
        Order order = Order.createNew(customerId, sampleAddress());

        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getItems()).isEmpty();
        assertThat(order.getDeliveryAddress()).isNotNull();
        assertThat(order.getDeliveryAddress().street()).isEqualTo("Rua das Flores");
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve lancar excecao ao criar pedido sem customerId ou sem endereço")
    void shouldThrowExceptionWhenCustomerIdOrAddressIsInvalid() {
        assertThatThrownBy(() -> Order.createNew(null, sampleAddress()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Order.createNew(UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve adicionar itens e recalcular o valor total do pedido")
    void shouldAddItemsAndRecalculateTotalAmount() {
        Order order = Order.createNew(UUID.randomUUID(), sampleAddress());
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
        Order order = Order.createNew(UUID.randomUUID(), sampleAddress());

        assertThatThrownBy(() -> order.addItem(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve remover item e recalcular o valor total do pedido")
    void shouldRemoveItemAndRecalculateTotalAmount() {
        Order order = Order.createNew(UUID.randomUUID(), sampleAddress());
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
        Order order = Order.createNew(UUID.randomUUID(), sampleAddress());

        assertThatThrownBy(() -> order.removeItem(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve marcar o pedido como pago apos transicao de RESERVED")
    void shouldMarkAsPaidWhenStatusIsReserved() {
        Order order = Order.createNew(UUID.randomUUID(), sampleAddress());

        order.markAsReserved();
        order.markAsPaid();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("Deve bloquear marcacao como pago quando estado nao e RESERVED")
    void shouldThrowExceptionWhenMarkingAsPaidFromInvalidState() {
        Order order = Order.createNew(UUID.randomUUID(), sampleAddress());

        assertThatThrownBy(order::markAsPaid)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot mark as paid from current state");
    }

    @Test
    @DisplayName("Deve cancelar o pedido com sucesso se nao estiver pago")
    void shouldCancelOrderSuccessfully() {
        Order order = Order.createNew(UUID.randomUUID(), sampleAddress());

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("Deve bloquear o cancelamento se o pedido ja estiver pago")
    void shouldThrowExceptionWhenCancelingAlreadyPaidOrder() {
        Order order = Order.createNew(UUID.randomUUID(), sampleAddress());
        order.markAsReserved();
        order.markAsPaid();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel an already paid");
    }
}
