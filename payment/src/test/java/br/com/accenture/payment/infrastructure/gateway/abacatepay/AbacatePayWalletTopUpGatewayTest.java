package br.com.accenture.payment.infrastructure.gateway.abacatepay;

import br.com.accenture.payment.application.port.WalletTopUpGateway;
import br.com.accenture.payment.domain.wallet.exception.InvalidTopUpRequestException;
import br.com.accenture.payment.infrastructure.config.AbacatePayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AbacatePayWalletTopUpGatewayTest {

    private static final String BASE_URL = "https://abacatepay.test";
    private static final BigDecimal FIXED_CHARGE = new BigDecimal("80.00");

    private MockRestServiceServer server;
    private AbacatePayWalletTopUpGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        AbacatePayProperties properties = new AbacatePayProperties("test-api-key", "test-secret", FIXED_CHARGE);
        gateway = new AbacatePayWalletTopUpGateway(builder, properties, BASE_URL);
    }

    @Test
    void createOrderReturnsGatewayResponseWhenAbacatePayResponds() {
        UUID topUpId = UUID.fromString("a1234567-89ab-cdef-0123-456789abcdef");
        UUID walletId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        UUID customerId = UUID.fromString("11111111-2222-3333-4444-555555555555");

        server.expect(requestTo(BASE_URL + "/v2/transparents/create"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(jsonPath("$.method").value("PIX"))
                .andExpect(jsonPath("$.data.amount").value(8000))
                .andExpect(jsonPath("$.data.description").value("Recarga de Wallet"))
                .andExpect(jsonPath("$.data.externalId").value(topUpId.toString()))
                .andExpect(jsonPath("$.data.metadata.topUpId").value(topUpId.toString()))
                .andRespond(withSuccess(
                        """
                        {
                          "data": {
                            "id": "bill_abc",
                            "amount": 8000,
                            "status": "PENDING",
                            "devMode": false,
                            "brCode": "br-code-123",
                            "brCodeBase64": "base64-456",
                            "expiresAt": "2026-05-20T10:00:00Z"
                          },
                          "success": true
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        WalletTopUpGateway.WalletTopUpGatewayResponse response = gateway.createOrder(
                new WalletTopUpGateway.WalletTopUpGatewayRequest(topUpId, walletId, customerId, FIXED_CHARGE, null)
        );

        assertThat(response.externalOrderId()).isEqualTo("bill_abc");
        assertThat(response.clientToken()).isNull();
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.amount()).isEqualByComparingTo(FIXED_CHARGE);
        assertThat(response.qrCode()).isEqualTo("br-code-123");
        assertThat(response.qrCodeBase64()).isEqualTo("base64-456");
        assertThat(response.ticketUrl()).isNull();
        server.verify();
    }

    @Test
    void createOrderThrowsInvalidTopUpWithErrorAsTextWhenAbacatePayReturns4xx() {
        server.expect(requestTo(BASE_URL + "/v2/transparents/create"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"cliente recusou pagamento\"}"));

        assertThatExceptionOfType(InvalidTopUpRequestException.class)
                .isThrownBy(() -> gateway.createOrder(sampleRequest()))
                .withMessage("cliente recusou pagamento");
    }

    @Test
    void createOrderThrowsInvalidTopUpWithNestedErrorMessageWhenErrorIsObject() {
        server.expect(requestTo(BASE_URL + "/v2/transparents/create"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":42,\"message\":\"erro nested\"}}"));

        assertThatExceptionOfType(InvalidTopUpRequestException.class)
                .isThrownBy(() -> gateway.createOrder(sampleRequest()))
                .withMessage("erro nested");
    }

    @Test
    void createOrderThrowsInvalidTopUpWithRootMessageWhenNoErrorNode() {
        server.expect(requestTo(BASE_URL + "/v2/transparents/create"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"erro root\"}"));

        assertThatExceptionOfType(InvalidTopUpRequestException.class)
                .isThrownBy(() -> gateway.createOrder(sampleRequest()))
                .withMessage("erro root");
    }

    @Test
    void createOrderThrowsInvalidTopUpWithDefaultMessageWhenBodyIsEmpty() {
        server.expect(requestTo(BASE_URL + "/v2/transparents/create"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatExceptionOfType(InvalidTopUpRequestException.class)
                .isThrownBy(() -> gateway.createOrder(sampleRequest()))
                .withMessage("Pagamento recusado pelo gateway de pagamento.");
    }

    @Test
    void createOrderThrowsInvalidTopUpReturningRawBodyWhenBodyIsNotJson() {
        server.expect(requestTo(BASE_URL + "/v2/transparents/create"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("not-a-json"));

        assertThatExceptionOfType(InvalidTopUpRequestException.class)
                .isThrownBy(() -> gateway.createOrder(sampleRequest()))
                .withMessage("not-a-json");
    }

    @Test
    void createOrderThrowsInvalidTopUpWithRawBodyWhenErrorNodeIsEmpty() {
        server.expect(requestTo(BASE_URL + "/v2/transparents/create"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"\"}"));

        assertThatExceptionOfType(InvalidTopUpRequestException.class)
                .isThrownBy(() -> gateway.createOrder(sampleRequest()))
                .withMessageContaining("error");
    }

    @Test
    void createOrderThrowsIllegalStateWhenAbacatePayReturnsResponseWithoutData() {
        server.expect(requestTo(BASE_URL + "/v2/transparents/create"))
                .andRespond(withSuccess(
                        "{\"success\":true}",
                        MediaType.APPLICATION_JSON
                ));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> gateway.createOrder(sampleRequest()))
                .withMessageContaining("did not return a response");
    }

    private static WalletTopUpGateway.WalletTopUpGatewayRequest sampleRequest() {
        return new WalletTopUpGateway.WalletTopUpGatewayRequest(
                UUID.fromString("a1234567-89ab-cdef-0123-456789abcdef"),
                UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                UUID.fromString("11111111-2222-3333-4444-555555555555"),
                FIXED_CHARGE,
                null
        );
    }
}
