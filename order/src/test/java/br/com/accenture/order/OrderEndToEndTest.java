package br.com.accenture.order;

import br.com.accenture.order.api.dto.request.OrderCreateRequest;
import br.com.accenture.order.api.dto.request.OrderItemRequest;
import br.com.accenture.order.api.dto.response.OrderResponse;
import br.com.accenture.order.domain.enums.OrderStatus;
import br.com.accenture.order.infrastructure.persistence.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(OrderEndToEndTest.AuditConfig.class)
class OrderEndToEndTest {

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditConfig {}

    @LocalServerPort
    private int port;

    @Autowired
    private OrderJpaRepository jpaRepository;

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
        var itemRequest = new OrderItemRequest("SKU-E2E", 3, new BigDecimal("100.00"));
        var createRequest = new OrderCreateRequest("customer-e2e", List.of(itemRequest));

        ResponseEntity<OrderResponse> createResponse = restClient.post()
                .uri("/orders")
                .body(createRequest)
                .retrieve()
                .toEntity(OrderResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().customerId()).isEqualTo("customer-e2e");
        assertThat(createResponse.getBody().totalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(createResponse.getBody().status()).isEqualTo(OrderStatus.PENDING.name());

        String orderUrl = "/orders/" + createResponse.getBody().orderId();

        ResponseEntity<OrderResponse> getResponse = restClient.get()
                .uri(orderUrl)
                .retrieve()
                .toEntity(OrderResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().customerId()).isEqualTo("customer-e2e");
        assertThat(getResponse.getBody().items()).hasSize(1);
        assertThat(getResponse.getBody().items().get(0).sku()).isEqualTo("SKU-E2E");
    }
}