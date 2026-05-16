package br.com.accenture.inventory.e2e;

import br.com.accenture.inventory.api.dto.request.ProductRequest;
import br.com.accenture.inventory.api.dto.request.StockReservationRequest;
import br.com.accenture.inventory.application.port.StockEventPublisher;
import br.com.accenture.inventory.domain.model.StockReservation;
import br.com.accenture.inventory.infrastructure.messaging.event.OrderCreatedEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.PaymentApprovedEvent;
import br.com.accenture.inventory.infrastructure.messaging.listener.OrderEventListener;
import br.com.accenture.inventory.infrastructure.messaging.listener.PaymentEventListener;
import br.com.accenture.inventory.infrastructure.persistence.ProductJpaRepository;
import br.com.accenture.inventory.infrastructure.persistence.StockReservationJpaRepository;
import br.com.accenture.inventory.infrastructure.persistence.entity.ProductJpaEntity;
import br.com.accenture.inventory.infrastructure.persistence.entity.StockReservationJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
@Transactional
class InventoryLifecycleE2eTest {

    private static final String INTERNAL_SECRET = "senha-secreta-microsservicos-1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private StockReservationJpaRepository stockReservationJpaRepository;

    @Autowired
    private PaymentEventListener paymentEventListener;

    @Autowired
    private OrderEventListener orderEventListener;

    @Autowired
    private CapturingStockEventPublisher stockEventPublisher;

    @BeforeEach
    void setUp() {
        stockEventPublisher.clear();
    }

    @Test
    void shouldCreateReservationAndConfirmByPaymentApprovedEvent() throws Exception {
        UUID orderId = UUID.randomUUID();
        String productId = createProduct("SKU-E2E-1", 10);
        String reservationId = createReservation(orderId, productId, 3);

        ProductJpaEntity afterReserve = productJpaRepository.findById(UUID.fromString(productId)).orElseThrow();
        assertThat(afterReserve.getStockQuantity()).isEqualTo(7);
        assertThat(stockEventPublisher.reservedReservationIds).containsExactly(UUID.fromString(reservationId));

        paymentEventListener.handlePaymentApproved(new PaymentApprovedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "PIX",
                Instant.now(),
                Instant.now()
        ));

        StockReservationJpaEntity reservation = stockReservationJpaRepository.findById(UUID.fromString(reservationId)).orElseThrow();
        assertThat(reservation.getStatus().name()).isEqualTo("CONFIRMED");
        ProductJpaEntity afterConfirm = productJpaRepository.findById(UUID.fromString(productId)).orElseThrow();
        assertThat(afterConfirm.getStockQuantity()).isEqualTo(7);
        assertThat(stockEventPublisher.confirmedReservationIds).containsExactly(UUID.fromString(reservationId));
    }

    @Test
    void shouldCancelActiveReservationAndRestoreStockByApiFlow() throws Exception {
        UUID orderId = UUID.randomUUID();
        String productId = createProduct("SKU-E2E-2", 8);
        String reservationId = createReservation(orderId, productId, 2);

        mockMvc.perform(patch("/stock-reservations/{id}/cancel", reservationId)
                        .with(internalSecret()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        ProductJpaEntity product = productJpaRepository.findById(UUID.fromString(productId)).orElseThrow();
        assertThat(product.getStockQuantity()).isEqualTo(8);
        assertThat(stockEventPublisher.canceledReservationIds).containsExactly(UUID.fromString(reservationId));
    }

    @Test
    void shouldPublishReservationFailedWhenOrderCreatedRequestsMoreStockThanAvailable() throws Exception {
        createProduct("SKU-E2E-3", 1);
        UUID orderId = UUID.randomUUID();

        orderEventListener.handleOrderCreated(new OrderCreatedEvent(
                orderId,
                "customer-2",
                new BigDecimal("50.00"),
                List.of(new OrderCreatedEvent.OrderItemEvent("SKU-E2E-3", 2))
        ));

        assertThat(stockEventPublisher.failedEvents).hasSize(1);
        FailedReservationEvent failedEvent = stockEventPublisher.failedEvents.getFirst();
        assertThat(failedEvent.orderId).isEqualTo(orderId);
        assertThat(failedEvent.sku).isEqualTo("SKU-E2E-3");
        assertThat(failedEvent.quantity).isEqualTo(2);
        assertThat(failedEvent.reason).contains("Insufficient stock");
    }

    private String createProduct(String sku, int stockQuantity) throws Exception {
        ProductRequest request = new ProductRequest(
                sku,
                "Produto " + sku,
                "Categoria",
                new BigDecimal("10.00"),
                stockQuantity,
                null
        );

        MvcResult result = mockMvc.perform(post("/products")
                        .with(internalSecret())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sku").value(sku))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createReservation(UUID orderId, String productId, int quantity) throws Exception {
        StockReservationRequest request = new StockReservationRequest(
                orderId,
                UUID.fromString(productId),
                quantity
        );

        MvcResult result = mockMvc.perform(post("/stock-reservations")
                        .with(internalSecret())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private static RequestPostProcessor internalSecret() {
        return request -> {
            request.addHeader("X-Internal-Secret", INTERNAL_SECRET);
            return request;
        };
    }

    @TestConfiguration
    static class InventoryLifecycleTestConfig {

        @Bean
        @Primary
        CapturingStockEventPublisher stockEventPublisher() {
            return new CapturingStockEventPublisher();
        }
    }

    static final class FailedReservationEvent {
        private final UUID orderId;
        private final String sku;
        private final Integer quantity;
        private final String reason;

        private FailedReservationEvent(UUID orderId, String sku, Integer quantity, String reason) {
            this.orderId = orderId;
            this.sku = sku;
            this.quantity = quantity;
            this.reason = reason;
        }
    }

    static final class CapturingStockEventPublisher implements StockEventPublisher {

        private final List<UUID> reservedReservationIds = new ArrayList<>();
        private final List<UUID> confirmedReservationIds = new ArrayList<>();
        private final List<UUID> canceledReservationIds = new ArrayList<>();
        private final List<FailedReservationEvent> failedEvents = new ArrayList<>();

        @Override
        public void publishStockReserved(StockReservation reservation) {
            reservedReservationIds.add(reservation.getId());
        }

        @Override
        public void publishStockReservationConfirmed(StockReservation reservation) {
            confirmedReservationIds.add(reservation.getId());
        }

        @Override
        public void publishStockReservationCanceled(StockReservation reservation) {
            canceledReservationIds.add(reservation.getId());
        }

        @Override
        public void publishStockReservationFailed(UUID orderId, String sku, Integer quantity, String reason) {
            failedEvents.add(new FailedReservationEvent(orderId, sku, quantity, reason));
        }

        void clear() {
            reservedReservationIds.clear();
            confirmedReservationIds.clear();
            canceledReservationIds.clear();
            failedEvents.clear();
        }
    }
}
