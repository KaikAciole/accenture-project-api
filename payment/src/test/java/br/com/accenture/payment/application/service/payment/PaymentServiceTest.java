package br.com.accenture.payment.application.service.payment;

import br.com.accenture.payment.application.port.PaymentEventPublisher;
import br.com.accenture.payment.application.service.wallet.WalletService;
import br.com.accenture.payment.domain.payment.enums.PaymentMethod;
import br.com.accenture.payment.domain.payment.enums.PaymentStatus;
import br.com.accenture.payment.domain.payment.exception.DuplicatePaymentException;
import br.com.accenture.payment.domain.payment.exception.InvalidPaymentStatusException;
import br.com.accenture.payment.domain.payment.exception.PaymentNotFoundException;
import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.payment.repository.PaymentRepository;
import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import br.com.accenture.payment.domain.wallet.model.Wallet;
import br.com.accenture.payment.domain.wallet.model.WalletTransaction;
import br.com.accenture.payment.domain.wallet.repository.WalletRepository;
import br.com.accenture.payment.domain.wallet.repository.WalletTransactionRepository;
import br.com.accenture.payment.infrastructure.config.PaymentWalletProperties;
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
    private static final UUID COMPANY_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final BigDecimal AMOUNT = new BigDecimal("149.90");

    private final FakePaymentRepository paymentRepository = new FakePaymentRepository();
    private final NoopWalletTransactionRepository walletTransactionRepository = new NoopWalletTransactionRepository();
    private final FakeWalletService walletService = new FakeWalletService();
    private final FakePaymentEventPublisher eventPublisher = new FakePaymentEventPublisher();
    private final PaymentService service = new PaymentService(
            paymentRepository,
            walletService,
            walletTransactionRepository,
            new PaymentWalletProperties(COMPANY_OWNER_ID),
            eventPublisher
    );

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
        paymentRepository.findByIdResponses.add(Optional.of(approvedWalletPayment()));

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
        assertThat(walletService.refundCalls).containsExactly(new RefundCall(
                COMPANY_OWNER_ID,
                CUSTOMER_ID,
                AMOUNT,
                PAYMENT_ID
        ));
        assertThat(eventPublisher.approvedPayments).containsExactly(approved);
        assertThat(eventPublisher.refusedPayments).containsExactly(refused);
        assertThat(eventPublisher.canceledPayments).containsExactly(canceled);
        assertThat(eventPublisher.refundedPayments).containsExactly(
                new RefundedPaymentCall(refunded, "Estorno solicitado manualmente via API")
        );
    }

    @Test
    void manualRefundReturnsSamePaymentWhenAlreadyRefunded() {
        Payment payment = refundedPayment();
        paymentRepository.findByIdResponses.add(Optional.of(payment));

        Payment result = service.refund(PAYMENT_ID);

        assertThat(result).isSameAs(payment);
        assertThat(paymentRepository.savedPayments).isEmpty();
        assertThat(walletService.refundCalls).isEmpty();
        assertThat(eventPublisher.refundedPayments).isEmpty();
    }

    @Test
    void manualRefundThrowsWhenPaymentIsNotApproved() {
        paymentRepository.findByIdResponses.add(Optional.of(pendingPayment()));

        assertThatExceptionOfType(InvalidPaymentStatusException.class)
                .isThrownBy(() -> service.refund(PAYMENT_ID))
                .withMessage("Cannot refund payment from current status: PENDING");
        assertThat(paymentRepository.savedPayments).isEmpty();
        assertThat(walletService.refundCalls).isEmpty();
        assertThat(eventPublisher.refundedPayments).isEmpty();
    }

    @Test
    void processWalletPaymentTransfersBalanceAndPublishesApprovedEvent() {
        Payment walletPayment = Payment.restore(
                PAYMENT_ID,
                ORDER_ID,
                CUSTOMER_ID,
                AMOUNT,
                PaymentMethod.WALLET,
                PaymentStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                null
        );
        paymentRepository.findByIdResponses.add(Optional.of(walletPayment));

        Payment result = service.process(PAYMENT_ID, null);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.getExternalTransactionId()).startsWith("WALLET-");
        assertThat(walletService.transferCalls).containsExactly(new TransferCall(
                CUSTOMER_ID,
                WalletOwnerType.CUSTOMER,
                COMPANY_OWNER_ID,
                WalletOwnerType.COMPANY,
                AMOUNT,
                PAYMENT_ID
        ));
        assertThat(eventPublisher.approvedPayments).containsExactly(result);
    }

    @Test
    void cancelByOrderIdCancelsPendingPaymentAndPublishesCanceledEvent() {
        Payment payment = pendingPayment();
        paymentRepository.findByOrderIdResponses.add(Optional.of(payment));

        service.cancelByOrderId(ORDER_ID, "Order canceled");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getFailureReason()).isEqualTo("Order canceled");
        assertThat(paymentRepository.savedPayments).containsExactly(payment);
        assertThat(eventPublisher.canceledPayments).containsExactly(payment);
    }

    @Test
    void cancelByOrderIdCancelsProcessingPaymentAndPublishesCanceledEvent() {
        Payment payment = processingPayment();
        paymentRepository.findByOrderIdResponses.add(Optional.of(payment));

        service.cancelByOrderId(ORDER_ID, "Order canceled");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getFailureReason()).isEqualTo("Order canceled");
        assertThat(paymentRepository.savedPayments).containsExactly(payment);
        assertThat(eventPublisher.canceledPayments).containsExactly(payment);
    }

    @Test
    void cancelByOrderIdRefundsApprovedWalletPaymentAndPublishesRefundedEvent() {
        Payment payment = approvedWalletPayment();
        paymentRepository.findByOrderIdResponses.add(Optional.of(payment));

        service.cancelByOrderId(ORDER_ID, "Order canceled");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(walletTransactionRepository.existsByPaymentIdAndReasonCalls)
                .containsExactly(new ExistsByPaymentIdAndReasonCall(PAYMENT_ID, WalletTransactionReason.REFUND));
        assertThat(walletService.refundCalls).containsExactly(new RefundCall(
                COMPANY_OWNER_ID,
                CUSTOMER_ID,
                AMOUNT,
                PAYMENT_ID
        ));
        assertThat(paymentRepository.savedPayments).containsExactly(payment);
        assertThat(eventPublisher.refundedPayments).containsExactly(new RefundedPaymentCall(payment, "Order canceled"));
    }

    @Test
    void cancelByOrderIdDoesNotMoveWalletAgainWhenRefundTransactionAlreadyExists() {
        Payment payment = approvedWalletPayment();
        walletTransactionRepository.existsByPaymentIdAndReason = true;
        paymentRepository.findByOrderIdResponses.add(Optional.of(payment));

        service.cancelByOrderId(ORDER_ID, "Order canceled");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(walletService.refundCalls).isEmpty();
        assertThat(paymentRepository.savedPayments).containsExactly(payment);
        assertThat(eventPublisher.refundedPayments).containsExactly(new RefundedPaymentCall(payment, "Order canceled"));
    }

    @Test
    void cancelByOrderIdDoesNothingWhenPaymentIsAlreadyFinalOrMissing() {
        paymentRepository.findByOrderIdResponses.add(Optional.of(refusedPayment()));
        paymentRepository.findByOrderIdResponses.add(Optional.of(canceledPayment()));
        paymentRepository.findByOrderIdResponses.add(Optional.of(refundedPayment()));
        paymentRepository.findByOrderIdResponses.add(Optional.empty());

        service.cancelByOrderId(ORDER_ID, "Order canceled");
        service.cancelByOrderId(ORDER_ID, "Order canceled");
        service.cancelByOrderId(ORDER_ID, "Order canceled");
        service.cancelByOrderId(ORDER_ID, "Order canceled");

        assertThat(paymentRepository.savedPayments).isEmpty();
        assertThat(eventPublisher.canceledPayments).isEmpty();
        assertThat(eventPublisher.refundedPayments).isEmpty();
        assertThat(walletService.refundCalls).isEmpty();
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

    private static Payment approvedWalletPayment() {
        return Payment.restore(
                PAYMENT_ID,
                ORDER_ID,
                CUSTOMER_ID,
                AMOUNT,
                PaymentMethod.WALLET,
                PaymentStatus.APPROVED,
                "WALLET-123",
                null,
                null,
                null,
                null,
                null
        );
    }

    private static Payment refusedPayment() {
        Payment payment = processingPayment();
        payment.refuse("Payment refused");
        return payment;
    }

    private static Payment canceledPayment() {
        Payment payment = pendingPayment();
        payment.cancel("Payment canceled");
        return payment;
    }

    private static Payment refundedPayment() {
        Payment payment = approvedWalletPayment();
        payment.refund();
        return payment;
    }

    private record TransferCall(
            UUID fromOwnerId,
            WalletOwnerType fromOwnerType,
            UUID toOwnerId,
            WalletOwnerType toOwnerType,
            BigDecimal amount,
            UUID paymentId
    ) {
    }

    private record RefundCall(
            UUID companyOwnerId,
            UUID customerId,
            BigDecimal amount,
            UUID paymentId
    ) {
    }

    private record RefundedPaymentCall(Payment payment, String reason) {
    }

    private record ExistsByPaymentIdAndReasonCall(UUID paymentId, WalletTransactionReason reason) {
    }

    private static final class FakeWalletService extends WalletService {

        private final List<TransferCall> transferCalls = new ArrayList<>();
        private final List<RefundCall> refundCalls = new ArrayList<>();

        private FakeWalletService() {
            super(new NoopWalletRepository(), new NoopWalletTransactionRepository());
        }

        @Override
        public void transfer(
                UUID fromOwnerId,
                WalletOwnerType fromOwnerType,
                UUID toOwnerId,
                WalletOwnerType toOwnerType,
                BigDecimal amount,
                UUID paymentId
        ) {
            transferCalls.add(new TransferCall(
                    fromOwnerId,
                    fromOwnerType,
                    toOwnerId,
                    toOwnerType,
                    amount,
                    paymentId
            ));
        }

        @Override
        public void refund(UUID companyOwnerId, UUID customerId, BigDecimal amount, UUID paymentId) {
            refundCalls.add(new RefundCall(companyOwnerId, customerId, amount, paymentId));
        }
    }

    private static final class FakePaymentEventPublisher implements PaymentEventPublisher {

        private final List<Payment> approvedPayments = new ArrayList<>();
        private final List<Payment> refusedPayments = new ArrayList<>();
        private final List<Payment> canceledPayments = new ArrayList<>();
        private final List<RefundedPaymentCall> refundedPayments = new ArrayList<>();

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

        @Override
        public void publishPaymentRefunded(Payment payment, String reason) {
            refundedPayments.add(new RefundedPaymentCall(payment, reason));
        }
    }

    private static final class NoopWalletRepository implements WalletRepository {

        @Override
        public Wallet save(Wallet wallet) {
            return wallet;
        }

        @Override
        public Optional<Wallet> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<Wallet> findByOwnerIdAndOwnerType(UUID ownerId, WalletOwnerType ownerType) {
            return Optional.empty();
        }

        @Override
        public boolean existsByOwnerIdAndOwnerType(UUID ownerId, WalletOwnerType ownerType) {
            return false;
        }
    }

    private static final class NoopWalletTransactionRepository implements WalletTransactionRepository {

        private boolean existsByPaymentIdAndReason;
        private final List<ExistsByPaymentIdAndReasonCall> existsByPaymentIdAndReasonCalls = new ArrayList<>();

        @Override
        public WalletTransaction save(WalletTransaction transaction) {
            return transaction;
        }

        @Override
        public PageResult<WalletTransaction> findByWalletId(UUID walletId, PageRequest pageRequest) {
            return new PageResult<>(List.of(), pageRequest.page(), pageRequest.size(), 0, 0);
        }

        @Override
        public boolean existsByPaymentIdAndReason(UUID paymentId, WalletTransactionReason reason) {
            existsByPaymentIdAndReasonCalls.add(new ExistsByPaymentIdAndReasonCall(paymentId, reason));
            return existsByPaymentIdAndReason;
        }
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
