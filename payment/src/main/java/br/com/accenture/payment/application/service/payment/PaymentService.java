package br.com.accenture.payment.application.service.payment;

import br.com.accenture.payment.application.service.wallet.WalletService;
import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.payment.enums.PaymentMethod;
import br.com.accenture.payment.domain.payment.exception.DuplicatePaymentException;
import br.com.accenture.payment.domain.payment.exception.PaymentNotFoundException;
import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.domain.payment.repository.PaymentRepository;
import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private static final String WALLET_TRANSACTION_PREFIX = "WALLET-";

    private final PaymentRepository paymentRepository;
    private final WalletService walletService;
    private final UUID companyWalletOwnerId;

    public PaymentService(
            PaymentRepository paymentRepository,
            WalletService walletService,
            @Value("${payment.wallet.company-owner-id}") UUID companyWalletOwnerId
    ) {
        this.paymentRepository = paymentRepository;
        this.walletService = walletService;
        this.companyWalletOwnerId = companyWalletOwnerId;
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

    @Transactional
    public Payment approve(UUID id) {
        Payment payment = loadById(id);

        payment.approve();

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment refuse(UUID id, String failureReason) {
        Payment payment = loadById(id);

        payment.refuse(failureReason);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment cancel(UUID id, String failureReason) {
        Payment payment = loadById(id);

        payment.cancel(failureReason);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment refund(UUID id) {
        Payment payment = loadById(id);

        payment.refund();

        return paymentRepository.save(payment);
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

    private Payment processWithWallet(Payment payment) {
        String internalTransactionId = generateWalletPaymentTransactionId();

        payment.process(internalTransactionId);

        walletService.transfer(
                payment.getCustomerId(),
                WalletOwnerType.CUSTOMER,
                companyWalletOwnerId,
                WalletOwnerType.COMPANY,
                payment.getAmount(),
                payment.getId()
        );

        payment.approve();

        return paymentRepository.save(payment);
    }

    private Payment processWithExternalGateway(Payment payment, String externalTransactionId) {
        validateExternalTransactionId(externalTransactionId);

        payment.process(externalTransactionId);

        return paymentRepository.save(payment);
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
}