package br.com.accenture.payment.application.service;

import br.com.accenture.payment.domain.enums.PaymentMethod;
import br.com.accenture.payment.domain.enums.PaymentStatus;
import br.com.accenture.payment.domain.exception.DuplicatePaymentException;
import br.com.accenture.payment.domain.exception.PaymentNotFoundException;
import br.com.accenture.payment.domain.model.Payment;
import br.com.accenture.payment.domain.repository.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private static final UUID PAYMENT_ID = UUID.fromString("6ca24443-0347-486b-b276-290f4170909f");
    private static final UUID ORDER_ID = UUID.fromString("e3bc2c53-e29c-4a19-9063-8b8cb55507d6");
    private static final UUID CUSTOMER_ID = UUID.fromString("2a497a58-b4e5-44ac-a79b-797ca294865e");
    private static final BigDecimal AMOUNT = new BigDecimal("149.90");

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentService service = new PaymentService(paymentRepository);

    @Test
    void createPersistsNewPaymentWhenOrderDoesNotHavePayment() {
        Payment saved = pendingPayment();
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        Payment result = service.create(ORDER_ID, CUSTOMER_ID, AMOUNT, PaymentMethod.PIX);

        assertThat(result).isSameAs(saved);
        verify(paymentRepository).existsByOrderId(ORDER_ID);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createThrowsWhenOrderAlreadyHasPayment() {
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(true);

        assertThatExceptionOfType(DuplicatePaymentException.class)
                .isThrownBy(() -> service.create(ORDER_ID, CUSTOMER_ID, AMOUNT, PaymentMethod.PIX))
                .withMessage("Payment already exists for order id: " + ORDER_ID);
        verify(paymentRepository).existsByOrderId(ORDER_ID);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void findByIdAndOrderIdReturnPaymentOrThrowWhenMissing() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment), Optional.empty());
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment), Optional.empty());

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
        Payment toProcess = pendingPayment();
        Payment toApprove = processingPayment();
        Payment toRefuse = processingPayment();
        Payment toCancel = pendingPayment();
        Payment toRefund = approvedPayment();
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(
                        Optional.of(toProcess),
                        Optional.of(toApprove),
                        Optional.of(toRefuse),
                        Optional.of(toCancel),
                        Optional.of(toRefund)
                );
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
    }

    @Test
    void deleteRequiresExistingPayment() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(pendingPayment()), Optional.empty());

        service.delete(PAYMENT_ID);

        verify(paymentRepository).deleteById(PAYMENT_ID);
        assertThatExceptionOfType(PaymentNotFoundException.class)
                .isThrownBy(() -> service.delete(PAYMENT_ID))
                .withMessage("Payment not found with id: " + PAYMENT_ID);
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
}
