package br.com.accenture.order.infrastructure.persistence;

import br.com.accenture.order.application.dto.PaginatedResult;
import br.com.accenture.order.domain.model.DeliveryAddress;
import br.com.accenture.order.domain.model.Order;
import br.com.accenture.order.domain.model.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({OrderRepositoryAdapter.class, OrderRepositoryAdapterTest.AuditConfig.class})
class OrderRepositoryAdapterTest {

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditConfig {}

    @Autowired
    private OrderRepositoryAdapter adapter;

    private static DeliveryAddress sampleAddress() {
        return new DeliveryAddress(
                "Rua das Flores", "123", "Apto 1", "Centro", "São Paulo", "SP", "01001000"
        );
    }

    @Test
    @DisplayName("Deve persistir um pedido e seus itens com sucesso e cascade")
    void shouldPersistOrderAndItemsSuccessfully() {
        Order order = Order.createNew(UUID.randomUUID(), sampleAddress());
        order.addItem(OrderItem.createNew("PROD-001", 2, new BigDecimal("75.00")));

        Order savedOrder = adapter.save(order);

        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(savedOrder.getItems()).hasSize(1);
        assertThat(savedOrder.getItems().get(0).getId()).isNotNull();
        assertThat(savedOrder.getDeliveryAddress()).isNotNull();
        assertThat(savedOrder.getDeliveryAddress().street()).isEqualTo("Rua das Flores");
    }

    @Test
    @DisplayName("Deve recuperar um pedido por ID com todos os itens mapeados")
    void shouldFindOrderById() {
        UUID customerId = UUID.randomUUID();
        Order order = Order.createNew(customerId, sampleAddress());
        order.addItem(OrderItem.createNew("ITEM-1", 1, BigDecimal.TEN));
        UUID id = adapter.save(order).getId();

        Optional<Order> foundOrder = adapter.findById(id);

        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getCustomerId()).isEqualTo(customerId);
        assertThat(foundOrder.get().getItems()).hasSize(1);
        assertThat(foundOrder.get().getDeliveryAddress().city()).isEqualTo("São Paulo");
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar ID inexistente")
    void shouldReturnEmptyWhenIdDoesNotExist() {
        Optional<Order> foundOrder = adapter.findById(UUID.randomUUID());
        assertThat(foundOrder).isEmpty();
    }

    @Test
    @DisplayName("Deve buscar pedidos paginados filtrando por customerId")
    void shouldFindOrdersByCustomerIdWithPagination() {
        UUID targetCustomer = UUID.randomUUID();
        adapter.save(Order.createNew(targetCustomer, sampleAddress()));
        adapter.save(Order.createNew(targetCustomer, sampleAddress()));
        adapter.save(Order.createNew(UUID.randomUUID(), sampleAddress()));

        PaginatedResult<Order> result = adapter.findByCustomerId(targetCustomer, 0, 10);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.data()).hasSize(2);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve deletar pedido e remover itens dependentes")
    void shouldDeleteOrderAndOrphanItems() {
        Order order = Order.createNew(UUID.randomUUID(), sampleAddress());
        order.addItem(OrderItem.createNew("ITEM-X", 1, BigDecimal.ONE));
        Order saved = adapter.save(order);

        adapter.deleteById(saved.getId());

        Optional<Order> found = adapter.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Deve listar todos os pedidos paginados independente do cliente")
    void shouldFindAllOrdersWithPagination() {
        adapter.save(Order.createNew(UUID.randomUUID(), sampleAddress()));
        adapter.save(Order.createNew(UUID.randomUUID(), sampleAddress()));
        adapter.save(Order.createNew(UUID.randomUUID(), sampleAddress()));

        PaginatedResult<Order> result = adapter.findAll(0, 10);

        assertThat(result.data()).hasSize(3);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve respeitar tamanho da pagina ao listar todos os pedidos")
    void shouldRespectPageSizeWhenListingAllOrders() {
        adapter.save(Order.createNew(UUID.randomUUID(), sampleAddress()));
        adapter.save(Order.createNew(UUID.randomUUID(), sampleAddress()));
        adapter.save(Order.createNew(UUID.randomUUID(), sampleAddress()));

        PaginatedResult<Order> firstPage = adapter.findAll(0, 2);

        assertThat(firstPage.data()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
    }
}