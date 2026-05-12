package br.com.accenture.payment;

import br.com.accenture.payment.api.dto.request.PaymentFailureRequest;
import br.com.accenture.payment.api.dto.request.PaymentProcessRequest;
import br.com.accenture.payment.api.dto.request.PaymentRequest;
import br.com.accenture.payment.domain.payment.enums.PaymentMethod;
import br.com.accenture.payment.infrastructure.persistence.payment.PaymentJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentLifecycleE2eTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentJpaRepository jpaRepository;

    @Test
    void shouldCreatePaymentAndRetrieveItByIdAndOrderId() throws Exception {
        PaymentRequest request = new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("125.50"),
                PaymentMethod.PIX
        );

        MvcResult created = mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.orderId").value(request.orderId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String paymentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asString();

        mockMvc.perform(get("/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.customerId").value(request.customerId().toString()));

        mockMvc.perform(get("/payments/orders/{orderId}", request.orderId()))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentProcessRequest("tx-e2e-approve"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.externalTransactionId").value("tx-e2e-approve"));

        mockMvc.perform(patch("/payments/{id}/approve", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.paidAt").exists());

        mockMvc.perform(patch("/payments/{id}/refund", paymentId))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentFailureRequest("Antifraud refused"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUSED"))
                .andExpect(jsonPath("$.failureReason").value("Antifraud refused"));

        mockMvc.perform(patch("/payments/{id}/cancel", canceledId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentFailureRequest("Order canceled"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.failureReason").value("Order canceled"));
    }

    @Test
    void shouldReturn422ForInvalidStatusTransitionAnd400ForInvalidPayload() throws Exception {
        String paymentId = createPayment(new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("99.00"),
                PaymentMethod.PIX
        ));

        mockMvc.perform(patch("/payments/{id}/approve", paymentId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid payment status"));

        mockMvc.perform(patch("/payments/{id}/process", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentProcessRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"));
    }

    @Test
    void shouldListAndDeletePayments() throws Exception {
        String paymentId = createPayment(new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("10.00"),
                PaymentMethod.PIX
        ));

        mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(paymentId));

        mockMvc.perform(delete("/payments/{id}", paymentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/payments/{id}", paymentId))
                .andExpect(status().isNotFound());
    }

    private String createPayment(PaymentRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        assert jpaRepository.existsByOrderId(request.orderId());
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asString();
    }
}
