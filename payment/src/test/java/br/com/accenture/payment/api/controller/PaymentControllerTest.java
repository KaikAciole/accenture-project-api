package br.com.accenture.payment.api.controller;

import br.com.accenture.payment.api.dto.request.PaymentFailureRequest;
import br.com.accenture.payment.api.dto.request.PaymentProcessRequest;
import br.com.accenture.payment.api.dto.request.PaymentRequest;
import br.com.accenture.payment.application.service.payment.PaymentService;
import br.com.accenture.payment.domain.payment.enums.PaymentMethod;
import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.domain.payment.repository.PaymentRepository;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentControllerTest {

    private final FakePaymentService paymentService = new FakePaymentService();
    private final PaymentController controller = new PaymentController(paymentService);

    @Test
    void createReturnsCreatedPayment() {
        paymentService.createResult = TestFixtures.pendingPayment();

        var response = controller.create(validCreateRequest());

        assertThat(response.id()).isEqualTo(TestFixtures.PAYMENT_ID);
        assertThat(response.orderId()).isEqualTo(TestFixtures.ORDER_ID);
        assertThat(response.customerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(response.method()).isEqualTo(PaymentMethod.PIX);
        assertThat(response.status().name()).isEqualTo("PENDING");
        assertThat(paymentService.createCalls).containsExactly(validCreateRequest());
    }

    @Test
    void findAllReturnsPagedPayments() {
        paymentService.findAllResult = new PageResult<>(List.of(TestFixtures.pendingPayment()), 0, 20, 1, 1);
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);

        var response = controller.findAll(pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(TestFixtures.PAYMENT_ID);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(paymentService.findAllRequests).hasSize(1);
    }

    @Test
    void findByIdAndOrderIdReturnPayment() {
        paymentService.findByIdResult = TestFixtures.pendingPayment();
        paymentService.findByOrderIdResult = TestFixtures.pendingPayment();

        var byId = controller.findById(TestFixtures.PAYMENT_ID);
        var byOrderId = controller.findByOrderId(TestFixtures.ORDER_ID);

        assertThat(byId.id()).isEqualTo(TestFixtures.PAYMENT_ID);
        assertThat(byOrderId.orderId()).isEqualTo(TestFixtures.ORDER_ID);
        assertThat(paymentService.findByIdCalls).containsExactly(TestFixtures.PAYMENT_ID);
        assertThat(paymentService.findByOrderIdCalls).containsExactly(TestFixtures.ORDER_ID);
    }

    @Test
    void statusEndpointsReturnUpdatedPayments() {
        paymentService.processResult = TestFixtures.processingPayment();
        paymentService.approveResult = TestFixtures.approvedPayment();
        paymentService.refuseResult = TestFixtures.refusedPayment();
        paymentService.cancelResult = TestFixtures.canceledPayment();
        paymentService.refundResult = TestFixtures.refundedPayment();

        var processed = controller.process(
                TestFixtures.PAYMENT_ID,
                new PaymentProcessRequest(TestFixtures.EXTERNAL_TRANSACTION_ID)
        );
        var approved = controller.approve(TestFixtures.PAYMENT_ID);
        var refused = controller.refuse(TestFixtures.PAYMENT_ID, new PaymentFailureRequest(TestFixtures.FAILURE_REASON));
        var canceled = controller.cancel(TestFixtures.PAYMENT_ID, new PaymentFailureRequest("Customer requested"));
        var refunded = controller.refund(TestFixtures.PAYMENT_ID);

        assertThat(processed.status().name()).isEqualTo("PROCESSING");
        assertThat(processed.externalTransactionId()).isEqualTo(TestFixtures.EXTERNAL_TRANSACTION_ID);
        assertThat(approved.status().name()).isEqualTo("APPROVED");
        assertThat(refused.status().name()).isEqualTo("REFUSED");
        assertThat(canceled.status().name()).isEqualTo("CANCELED");
        assertThat(refunded.status().name()).isEqualTo("REFUNDED");
    }

    @Test
    void deleteDelegatesToService() {
        controller.delete(TestFixtures.PAYMENT_ID);

        assertThat(paymentService.deleteCalls).containsExactly(TestFixtures.PAYMENT_ID);
    }

    private static PaymentRequest validCreateRequest() {
        return new PaymentRequest(
                TestFixtures.ORDER_ID,
                TestFixtures.CUSTOMER_ID,
                TestFixtures.AMOUNT,
                PaymentMethod.PIX
        );
    }

    private static final class FakePaymentService extends PaymentService {

        private Payment createResult;
        private Payment findByIdResult;
        private Payment findByOrderIdResult;
        private Payment processResult;
        private Payment approveResult;
        private Payment refuseResult;
        private Payment cancelResult;
        private Payment refundResult;
        private PageResult<Payment> findAllResult;
        private final List<PaymentRequest> createCalls = new ArrayList<>();
        private final List<UUID> findByIdCalls = new ArrayList<>();
        private final List<UUID> findByOrderIdCalls = new ArrayList<>();
        private final List<UUID> deleteCalls = new ArrayList<>();
        private final List<PageRequest> findAllRequests = new ArrayList<>();

        private FakePaymentService() {
            super(new NoopPaymentRepository());
        }

        @Override
        public Payment create(UUID orderId,
                              UUID customerId,
                              java.math.BigDecimal amount,
                              PaymentMethod method) {
            createCalls.add(new PaymentRequest(orderId, customerId, amount, method));
            return createResult;
        }

        @Override
        public PageResult<Payment> findAll(PageRequest pageRequest) {
            findAllRequests.add(pageRequest);
            return findAllResult;
        }

        @Override
        public Payment findById(UUID id) {
            findByIdCalls.add(id);
            return findByIdResult;
        }

        @Override
        public Payment findByOrderId(UUID orderId) {
            findByOrderIdCalls.add(orderId);
            return findByOrderIdResult;
        }

        @Override
        public Payment process(UUID id, String externalTransactionId) {
            return processResult;
        }

        @Override
        public Payment approve(UUID id) {
            return approveResult;
        }

        @Override
        public Payment refuse(UUID id, String failureReason) {
            return refuseResult;
        }

        @Override
        public Payment cancel(UUID id, String failureReason) {
            return cancelResult;
        }

        @Override
        public Payment refund(UUID id) {
            return refundResult;
        }

        @Override
        public void delete(UUID id) {
            deleteCalls.add(id);
        }
    }

    private static final class NoopPaymentRepository implements PaymentRepository {

        @Override
        public Payment save(Payment payment) {
            return payment;
        }

        @Override
        public java.util.Optional<Payment> findById(UUID id) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<Payment> findByOrderId(UUID orderId) {
            return java.util.Optional.empty();
        }

        @Override
        public boolean existsByOrderId(UUID orderId) {
            return false;
        }

        @Override
        public PageResult<Payment> findAll(PageRequest pageRequest) {
            return new PageResult<>(List.of(), pageRequest.page(), pageRequest.size(), 0, 0);
        }

        @Override
        public void deleteById(UUID id) {
        }
    }
}
