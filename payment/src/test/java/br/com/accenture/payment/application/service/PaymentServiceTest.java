package br.com.accenture.payment.application.service;

import br.com.accenture.payment.domain.enums.PaymentMethod;
import br.com.accenture.payment.domain.enums.PaymentStatus;
import br.com.accenture.payment.domain.exception.DuplicatePaymentException;
import br.com.accenture.payment.domain.exception.InvalidPaymentStatusException;
import br.com.accenture.payment.domain.exception.PaymentNotFoundException;
import br.com.accenture.payment.domain.model.Payment;
import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.repository.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PaymentServiceTest {

    private static final UUID PAYMENT_ID = UUID.fromString("6ca24443-0347-486b-b276-290f4170909f");
    private static final UUID ORDER_ID = UUID.fromString("e3bc2c53-e29c-4a19-9063-8b8cb55507d6");
    private static final UUID CUSTOMER_ID = UUID.fromString("2a497a58-b4e5-44ac-a79b-797ca294865e");
    private static final BigDecimal AMOUNT = new BigDecimal("149.90");

    private final FakePaymentRepository paymentRepository = new FakePaymentRepository();
    private final PaymentService service = new PaymentService(paymentRepository);

    @Test
    void createPersistsNewPaymentWhenOrderDoesNotHavePayment() {
        Payment saved = pendingPayment();
        paymentRepository.existsByOrderId = false;
        paymentRepository.nextSavedPayment = saved;

        Payment result = service.create(ORDER_ID, CUSTOMER_ID, AMOUNT, PaymentMethod.PIX);

        assertThat(result).isSameAs(saved);
        assertThat(paymentRepository.existsByOrderIdCalls).containsExactly(ORDER_ID);
        assertThat(paymentRepository.savedPayments).hasSize(1);
    }

    @Test
    void createThrowsWhenOrderAlreadyHasPayment() {
        paymentRepository.existsByOrderId = true;

        assertThatExceptionOfType(DuplicatePaymentException.class)
                .isThrownBy(() -> service.create(ORDER_ID, CUSTOMER_ID, AMOUNT, PaymentMethod.PIX))
                .withMessage("Payment already exists for order id: " + ORDER_ID);
        assertThat(paymentRepository.existsByOrderIdCalls).containsExactly(ORDER_ID);
        assertThat(paymentRepository.savedPayments).isEmpty();
    }

    @Test
    void findByIdAndOrderIdReturnPaymentOrThrowWhenMissing() {
        Payment payment = pendingPayment();
        paymentRepository.findByIdResponses.add(Optional.of(payment));
        paymentRepository.findByIdResponses.add(Optional.empty());
        paymentRepository.findByOrderIdResponses.add(Optional.of(payment));
        paymentRepository.findByOrderIdResponses.add(Optional.empty());

        assertThat(service.findById(PAYMENT_ID)).isSameAs(payment);
        assertThat(service.findByOrderId(ORDER_ID)).isSameAs(payment);
        assertThatExceptionOfType(PaymentNotFoundException.class)
                .isThrownBy(() -> service.findById(PAYMENT_ID))
                .withMessage("Payment not found with id: " + PAYMENT_ID);
        assertThatExceptionOfType(PaymentNotFoundException.class)
                .isThrownBy(() -> service.findByOrderId(ORDER_ID))
                .withMessage("Payment not found with order id: " + ORDER_ID);
    }

    @Test
    void processApproveRefuseCancelAndRefundApplyStateChanges() {
        paymentRepository.findByIdResponses.add(Optional.of(pendingPayment()));
        paymentRepository.findByIdResponses.add(Optional.of(processingPayment()));
        paymentRepository.findByIdResponses.add(Optional.of(processingPayment()));
        paymentRepository.findByIdResponses.add(Optional.of(pendingPayment()));
        paymentRepository.findByIdResponses.add(Optional.of(approvedPayment()));

        Payment processing = service.process(PAYMENT_ID, "tx-123");
        Payment approved = service.approve(PAYMENT_ID);
        Payment refused = service.refuse(PAYMENT_ID, "Card declined");
        Payment canceled = service.cancel(PAYMENT_ID, "Customer requested");
        Payment refunded = service.refund(PAYMENT_ID);

        assertThat(processing.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(processing.getExternalTransactionId()).isEqualTo("tx-123");
        assertThat(approved.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(approved.getPaidAt()).isNotNull();
        assertThat(refused.getStatus()).isEqualTo(PaymentStatus.REFUSED);
        assertThat(refused.getFailureReason()).isEqualTo("Card declined");
        assertThat(canceled.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(canceled.getFailureReason()).isEqualTo("Customer requested");
        assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(paymentRepository.savedPayments).hasSize(5);
    }

    @Test
    void deleteRequiresExistingPayment() {
        paymentRepository.findByIdResponses.add(Optional.of(pendingPayment()));
        paymentRepository.findByIdResponses.add(Optional.empty());

        service.delete(PAYMENT_ID);

        assertThat(paymentRepository.deletedIds).containsExactly(PAYMENT_ID);
        assertThatExceptionOfType(PaymentNotFoundException.class)
                .isThrownBy(() -> service.delete(PAYMENT_ID))
                .withMessage("Payment not found with id: " + PAYMENT_ID);
    }

    @Test
    void findAllDelegatesToRepository() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageResult<Payment> page = new PageResult<>(List.of(pendingPayment()), 0, 10, 1, 1);
        paymentRepository.pageResult = page;

        PageResult<Payment> result = service.findAll(pageRequest);

        assertThat(result).isSameAs(page);
        assertThat(paymentRepository.findAllRequests).containsExactly(pageRequest);
    }

    @Test
    void statusOperationsPropagateDomainExceptionsAndDoNotSaveInvalidState() {
        paymentRepository.findByIdResponses.add(Optional.of(approvedPayment()));

        assertThatExceptionOfType(InvalidPaymentStatusException.class)
                .isThrownBy(() -> service.process(PAYMENT_ID, "tx-999"))
                .withMessage("Cannot process payment from current status: APPROVED");
        assertThat(paymentRepository.savedPayments).isEmpty();
    }

    private static Payment pendingPayment() {
        return Payment.restore(
                PAYMENT_ID,
                ORDER_ID,
                CUSTOMER_ID,
                AMOUNT,
                PaymentMethod.PIX,
                PaymentStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static Payment processingPayment() {
        return Payment.restore(
                PAYMENT_ID,
                ORDER_ID,
                CUSTOMER_ID,
                AMOUNT,
                PaymentMethod.PIX,
                PaymentStatus.PROCESSING,
                "tx-123",
                null,
                null,
                null,
                null,
                null
        );
    }

    private static Payment approvedPayment() {
        return Payment.restore(
                PAYMENT_ID,
                ORDER_ID,
                CUSTOMER_ID,
                AMOUNT,
                PaymentMethod.PIX,
                PaymentStatus.APPROVED,
                "tx-123",
                null,
                null,
                null,
                null,
                null
        );
    }

    private static final class FakePaymentRepository implements PaymentRepository {

        private boolean existsByOrderId;
        private Payment nextSavedPayment;
        private PageResult<Payment> pageResult;
        private final List<UUID> existsByOrderIdCalls = new ArrayList<>();
        private final List<Payment> savedPayments = new ArrayList<>();
        private final List<UUID> deletedIds = new ArrayList<>();
        private final List<PageRequest> findAllRequests = new ArrayList<>();
        private final Queue<Optional<Payment>> findByIdResponses = new ArrayDeque<>();
        private final Queue<Optional<Payment>> findByOrderIdResponses = new ArrayDeque<>();

        @Override
        public Payment save(Payment payment) {
            savedPayments.add(payment);
            return nextSavedPayment != null ? nextSavedPayment : payment;
        }

        @Override
        public Optional<Payment> findById(UUID id) {
            return findByIdResponses.isEmpty() ? Optional.empty() : findByIdResponses.remove();
        }

        @Override
        public Optional<Payment> findByOrderId(UUID orderId) {
            return findByOrderIdResponses.isEmpty() ? Optional.empty() : findByOrderIdResponses.remove();
        }

        @Override
        public boolean existsByOrderId(UUID orderId) {
            existsByOrderIdCalls.add(orderId);
            return existsByOrderId;
        }

        @Override
        public PageResult<Payment> findAll(PageRequest pageRequest) {
            findAllRequests.add(pageRequest);
            return pageResult;
        }

        @Override
        public void deleteById(UUID id) {
            deletedIds.add(id);
        }
    }
}
