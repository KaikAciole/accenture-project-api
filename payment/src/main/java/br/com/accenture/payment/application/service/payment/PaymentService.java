package br.com.accenture.payment.application.service.payment;

import br.com.accenture.payment.application.port.PaymentEventPublisher;
import br.com.accenture.payment.application.service.wallet.WalletService;
import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.payment.enums.PaymentMethod;
import br.com.accenture.payment.domain.payment.enums.PaymentStatus;
import br.com.accenture.payment.domain.payment.exception.DuplicatePaymentException;
import br.com.accenture.payment.domain.payment.exception.InvalidPaymentStatusException;
import br.com.accenture.payment.domain.payment.exception.PaymentNotFoundException;
import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.domain.payment.repository.PaymentRepository;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.repository.WalletTransactionRepository;
import br.com.accenture.payment.infrastructure.config.PaymentWalletProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private static final String WALLET_TRANSACTION_PREFIX = "WALLET-";

    private final PaymentRepository paymentRepository;
    private final WalletService walletService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PaymentWalletProperties paymentWalletProperties;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentService(
            PaymentRepository paymentRepository,
            WalletService walletService,
            WalletTransactionRepository walletTransactionRepository,
            PaymentWalletProperties paymentWalletProperties,
            PaymentEventPublisher paymentEventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.walletService = walletService;
        this.walletTransactionRepository = walletTransactionRepository;
        this.paymentWalletProperties = paymentWalletProperties;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Transactional
    public Payment create(UUID orderId, UUID customerId, BigDecimal amount, PaymentMethod method) {
        validateOrderPaymentDoesNotExist(orderId);

        Payment payment = Payment.createNew(orderId, customerId, amount, method);

        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment findById(UUID id) {
        return loadById(id);
    }

    @Transactional(readOnly = true)
    public Payment findByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> PaymentNotFoundException.byOrderId(orderId));
    }

    @Transactional
    public Payment process(UUID id, String externalTransactionId) {
        Payment payment = loadById(id);

        if (payment.getMethod() == PaymentMethod.WALLET) {
            return processWithWallet(payment);
        }

        return processWithExternalGateway(payment, externalTransactionId);
    }

    private Payment processWithWallet(Payment payment) {
        String internalTransactionId = generateWalletPaymentTransactionId();

        payment.process(internalTransactionId);

        walletService.transfer(
                payment.getCustomerId(),
                WalletOwnerType.CUSTOMER,
                paymentWalletProperties.companyOwnerId(),
                WalletOwnerType.COMPANY,
                payment.getAmount(),
                payment.getId()
        );

        payment.approve();

        Payment savedPayment = paymentRepository.save(payment);

        paymentEventPublisher.publishPaymentApproved(savedPayment);

        return savedPayment;
    }

    private Payment processWithExternalGateway(Payment payment, String externalTransactionId) {
        validateExternalTransactionId(externalTransactionId);

        payment.process(externalTransactionId);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment approve(UUID id) {
        Payment payment = loadById(id);

        payment.approve();

        Payment savedPayment = paymentRepository.save(payment);

        paymentEventPublisher.publishPaymentApproved(savedPayment);

        return savedPayment;
    }

    @Transactional
    public Payment refuse(UUID id, String failureReason) {
        Payment payment = loadById(id);

        payment.refuse(failureReason);

        Payment savedPayment = paymentRepository.save(payment);

        paymentEventPublisher.publishPaymentRefused(savedPayment);

        return savedPayment;
    }

    @Transactional
    public Payment cancel(UUID id, String failureReason) {
        Payment payment = loadById(id);

        payment.cancel(failureReason);

        Payment savedPayment = paymentRepository.save(payment);

        paymentEventPublisher.publishPaymentCanceled(savedPayment);

        return savedPayment;
    }

    @Transactional
    public void cancelByOrderId(UUID orderId, String reason) {
        paymentRepository.findByOrderId(orderId)
                .ifPresent(payment -> handleOrderCanceledPayment(payment, reason));
    }

    @Transactional
    public Payment refund(UUID id) {
        Payment payment = loadById(id);

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return payment;
        }

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new InvalidPaymentStatusException(payment.getStatus(), "refund");
        }

        return processApprovedPaymentRefund(payment, "Estorno solicitado manualmente via API");
    }

    @Transactional
    public void delete(UUID id) {
        Payment payment = loadById(id);

        paymentRepository.deleteById(payment.getId());
    }

    @Transactional(readOnly = true)
    public PageResult<Payment> findAll(PageRequest pageRequest) {
        return paymentRepository.findAll(pageRequest);
    }

    private Payment loadById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private void validateOrderPaymentDoesNotExist(UUID orderId) {
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new DuplicatePaymentException(orderId);
        }
    }

    private void validateExternalTransactionId(String externalTransactionId) {
        if (externalTransactionId == null || externalTransactionId.isBlank()) {
            throw new IllegalArgumentException("External transaction id is required for external payment methods");
        }
    }

    private String generateWalletPaymentTransactionId() {
        return WALLET_TRANSACTION_PREFIX + UUID.randomUUID();
    }

    private void handleOrderCanceledPayment(Payment payment, String reason) {
        if (payment.getStatus() == PaymentStatus.PENDING ||
                payment.getStatus() == PaymentStatus.PROCESSING) {
            cancelPaymentFromOrderCancellation(payment, reason);
            return;
        }

        if (payment.getStatus() == PaymentStatus.APPROVED) {
            processApprovedPaymentRefund(payment, reason);
        }
    }

    private void cancelPaymentFromOrderCancellation(Payment payment, String reason) {
        payment.cancel(reason);

        Payment savedPayment = paymentRepository.save(payment);

        paymentEventPublisher.publishPaymentCanceled(savedPayment);
    }

    private Payment processApprovedPaymentRefund(Payment payment, String reason) {
        if (payment.getMethod() == PaymentMethod.WALLET) {
            boolean refundAlreadyExists = walletTransactionRepository.existsByPaymentIdAndReason(
                    payment.getId(),
                    WalletTransactionReason.REFUND
            );

            if (!refundAlreadyExists) {
                refundWalletPayment(payment);
            }
        }

        payment.refund();

        Payment savedPayment = paymentRepository.save(payment);

        paymentEventPublisher.publishPaymentRefunded(savedPayment, reason);

        return savedPayment;
    }

    private void refundWalletPayment(Payment payment) {
        if (payment.getMethod() != PaymentMethod.WALLET) {
            throw new IllegalArgumentException("Refund is only supported for wallet payments");
        }

        walletService.refund(
                paymentWalletProperties.companyOwnerId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getId()
        );
    }
}
