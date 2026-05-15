package br.com.accenture.order;

import br.com.accenture.order.api.dto.request.OrderCreateRequest;
import br.com.accenture.order.api.dto.request.OrderItemRequest;
import br.com.accenture.order.api.dto.response.OrderResponse;
import br.com.accenture.order.domain.enums.OrderStatus;
import br.com.accenture.order.infrastructure.feign.CustomerClient;
import br.com.accenture.order.infrastructure.feign.InventoryClient;
import br.com.accenture.order.infrastructure.feign.dto.CustomerAddressResponse;
import br.com.accenture.order.infrastructure.feign.dto.ProductAvailabilityItemResponse;
import br.com.accenture.order.infrastructure.feign.dto.ProductAvailabilityRequest;
import br.com.accenture.order.infrastructure.persistence.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(OrderEndToEndTest.AuditConfig.class)
@Disabled("Requer RabbitMQ disponível com filas declaradas (ex.: order.stock.failed.queue) — rodar manualmente quando broker estiver pronto")
class OrderEndToEndTest {

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditConfig {}

    @LocalServerPort
    private int port;

    @Autowired
    private OrderJpaRepository jpaRepository;

    @MockitoBean
    private br.com.accenture.order.application.publisher.OrderEventPublisher eventPublisher;

    @MockitoBean
    private CustomerClient customerClient;

    @MockitoBean
    private InventoryClient inventoryClient;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();

        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    @DisplayName("Fluxo completo: Criar pedido, salvar no banco real e buscar pela API")
    void shouldExecuteFullOrderCreationAndRetrievalFlow() {
        UUID customerId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        when(customerClient.getAddress(any(), any(), any())).thenReturn(new CustomerAddressResponse(
                addressId, customerId,
                "Rua das Flores", "123", "Apto 1",
                "Centro", "São Paulo", "SP", "01001000"
        ));

        when(inventoryClient.checkAvailability(any(ProductAvailabilityRequest.class), any()))
                .thenReturn(List.of(new ProductAvailabilityItemResponse("SKU-E2E", 3, 10, true)));

        var itemRequest = new OrderItemRequest("SKU-E2E", 3, new BigDecimal("100.00"));
        var createRequest = new OrderCreateRequest(addressId, List.of(itemRequest));

        ResponseEntity<OrderResponse> createResponse = restClient.post()
                .uri("/orders")
                .header("X-Customer-Id", customerId.toString())
                .body(createRequest)
                .retrieve()
                .toEntity(OrderResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().customerId()).isEqualTo(customerId);
        assertThat(createResponse.getBody().totalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(createResponse.getBody().status()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(createResponse.getBody().deliveryAddress()).isNotNull();
        assertThat(createResponse.getBody().deliveryAddress().street()).isEqualTo("Rua das Flores");

        String orderUrl = "/orders/" + createResponse.getBody().orderId();

        ResponseEntity<OrderResponse> getResponse = restClient.get()
                .uri(orderUrl)
                .retrieve()
                .toEntity(OrderResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().customerId()).isEqualTo(customerId);
        assertThat(getResponse.getBody().items()).hasSize(1);
        assertThat(getResponse.getBody().items().get(0).sku()).isEqualTo("SKU-E2E");
    }
}
