package br.com.accenture.payment.e2e.payment;

import br.com.accenture.payment.api.payment.dto.request.PaymentFailureRequest;
import br.com.accenture.payment.api.payment.dto.request.PaymentProcessRequest;
import br.com.accenture.payment.api.payment.dto.request.PaymentRequest;
import br.com.accenture.payment.application.port.PaymentEventPublisher;
import br.com.accenture.payment.domain.payment.enums.PaymentMethod;
import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.infrastructure.persistence.payment.PaymentJpaRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
@Transactional
class PaymentLifecycleE2eTest {

    private static final String INTERNAL_SECRET = "senha-secreta-microsservicos-1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentJpaRepository jpaRepository;

    @Autowired
    private CapturingPaymentEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher.clear();
    }

    @Test
    void shouldCreatePaymentAndRetrieveItByIdAndOrderId() throws Exception {
        PaymentRequest request = new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("125.50"),
                PaymentMethod.PIX
        );

        MvcResult created = mockMvc.perform(post("/payments")
                        .with(internalSecret())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.orderId").value(request.orderId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String paymentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asString();

        mockMvc.perform(get("/payments/{id}", paymentId).with(internalSecret()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.customerId").value(request.customerId().toString()));

        mockMvc.perform(get("/payments/orders/{orderId}", request.orderId()).with(internalSecret()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId));
    }

    @Test
    void shouldReturn409WhenCreatingDuplicateOrderPayment() throws Exception {
        PaymentRequest request = new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("80.00"),
                PaymentMethod.CREDIT_CARD
        );
        createPayment(request);

        mockMvc.perform(post("/payments")
                        .with(internalSecret())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate payment"));
    }

    @Test
    void shouldExecuteProcessApproveAndRefundFlow() throws Exception {
        String paymentId = createPayment(new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("300.00"),
                PaymentMethod.DEBIT_CARD
        ));

        mockMvc.perform(patch("/payments/{id}/process", paymentId)
                        .with(internalSecret())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentProcessRequest("tx-e2e-approve"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.externalTransactionId").value("tx-e2e-approve"));

        mockMvc.perform(patch("/payments/{id}/approve", paymentId).with(internalSecret()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.paidAt").exists());

        assertThat(eventPublisher.approvedPayments).hasSize(1);

        mockMvc.perform(patch("/payments/{id}/refund", paymentId).with(internalSecret()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void shouldExecuteRefuseAndCancelFlows() throws Exception {
        String refusedId = createPayment(new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("45.00"),
                PaymentMethod.PIX
        ));
        String canceledId = createPayment(new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("65.00"),
                PaymentMethod.PIX
        ));

        mockMvc.perform(patch("/payments/{id}/refuse", refusedId)
                        .with(internalSecret())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentFailureRequest("Antifraud refused"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUSED"))
                .andExpect(jsonPath("$.failureReason").value("Antifraud refused"));

        assertThat(eventPublisher.refusedPayments).hasSize(1);

        mockMvc.perform(patch("/payments/{id}/cancel", canceledId)
                        .with(internalSecret())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentFailureRequest("Order canceled"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.failureReason").value("Order canceled"));

        assertThat(eventPublisher.canceledPayments).hasSize(1);
    }

    @Test
    void shouldReturn422ForInvalidStatusTransitionAnd400ForInvalidPayload() throws Exception {
        String paymentId = createPayment(new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("99.00"),
                PaymentMethod.PIX
        ));

        mockMvc.perform(patch("/payments/{id}/approve", paymentId).with(internalSecret()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid payment status"));

        mockMvc.perform(patch("/payments/{id}/process", paymentId)
                        .with(internalSecret())
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PaymentProcessRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void shouldListAndDeletePayments() throws Exception {
        String paymentId = createPayment(new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("10.00"),
                PaymentMethod.PIX
        ));

        mockMvc.perform(get("/payments").with(internalSecret()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(paymentId));

        mockMvc.perform(delete("/payments/{id}", paymentId).with(internalSecret()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/payments/{id}", paymentId).with(internalSecret()))
                .andExpect(status().isNotFound());
    }

    private String createPayment(PaymentRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/payments")
                        .with(internalSecret())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        assert jpaRepository.existsByOrderId(request.orderId());
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asString();
    }

    private static RequestPostProcessor internalSecret() {
        return request -> {
            request.addHeader("X-Internal-Secret", INTERNAL_SECRET);
            return request;
        };
    }

    @TestConfiguration
    static class PaymentLifecycleTestConfig {

        @Bean
        @Primary
        CapturingPaymentEventPublisher paymentEventPublisher() {
            return new CapturingPaymentEventPublisher();
        }
    }

    static final class CapturingPaymentEventPublisher implements PaymentEventPublisher {

        private final List<Payment> approvedPayments = new ArrayList<>();
        private final List<Payment> refusedPayments = new ArrayList<>();
        private final List<Payment> canceledPayments = new ArrayList<>();

        @Override
        public void publishPaymentApproved(Payment payment) {
            approvedPayments.add(payment);
        }

        @Override
        public void publishPaymentRefused(Payment payment) {
            refusedPayments.add(payment);
        }

        @Override
        public void publishPaymentCanceled(Payment payment) {
            canceledPayments.add(payment);
        }

        void clear() {
            approvedPayments.clear();
            refusedPayments.clear();
            canceledPayments.clear();
        }
    }
}
