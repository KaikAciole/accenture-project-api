package br.com.accenture.payment.domain.model;

import br.com.accenture.payment.domain.payment.enums.PaymentMethod;
import br.com.accenture.payment.domain.payment.enums.PaymentStatus;
import br.com.accenture.payment.domain.payment.exception.InvalidPaymentStatusException;
import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PaymentTest {

    @Test
    void createNewBuildsPendingPayment() {
        Payment payment = Payment.createNew(
                TestFixtures.ORDER_ID,
                TestFixtures.CUSTOMER_ID,
                TestFixtures.AMOUNT,
                PaymentMethod.CREDIT_CARD
        );

        assertThat(payment.getId()).isNull();
        assertThat(payment.getOrderId()).isEqualTo(TestFixtures.ORDER_ID);
        assertThat(payment.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(payment.getAmount()).isEqualByComparingTo(TestFixtures.AMOUNT);
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getExternalTransactionId()).isNull();
        assertThat(payment.getFailureReason()).isNull();
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    void createNewValidatesRequiredFields() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Payment.createNew(null, TestFixtures.CUSTOMER_ID, TestFixtures.AMOUNT, PaymentMethod.PIX))
                .withMessage("orderId must not be null");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Payment.createNew(TestFixtures.ORDER_ID, null, TestFixtures.AMOUNT, PaymentMethod.PIX))
                .withMessage("customerId must not be null");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Payment.createNew(TestFixtures.ORDER_ID, TestFixtures.CUSTOMER_ID, null, PaymentMethod.PIX))
                .withMessage("amount must be greater than zero");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Payment.createNew(TestFixtures.ORDER_ID, TestFixtures.CUSTOMER_ID, BigDecimal.ZERO, PaymentMethod.PIX))
                .withMessage("amount must be greater than zero");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Payment.createNew(TestFixtures.ORDER_ID, TestFixtures.CUSTOMER_ID, TestFixtures.AMOUNT, null))
                .withMessage("method must not be null");
    }

    @Test
    void restoreValidatesRequiredFields() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Payment.restore(null, TestFixtures.ORDER_ID, TestFixtures.CUSTOMER_ID,
                        TestFixtures.AMOUNT, PaymentMethod.PIX, PaymentStatus.PENDING, null, null, null, null, null, null))
                .withMessage("id must not be null");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Payment.restore(UUID.randomUUID(), TestFixtures.ORDER_ID, TestFixtures.CUSTOMER_ID,
                        TestFixtures.AMOUNT, PaymentMethod.PIX, null, null, null, null, null, null, null))
                .withMessage("status must not be null");
    }

    @Test
    void processMovesPendingPaymentToProcessing() {
        Payment payment = TestFixtures.pendingPayment();

        payment.process("tx-999");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(payment.getExternalTransactionId()).isEqualTo("tx-999");
        assertThat(payment.getFailureReason()).isNull();
    }

    @Test
    void approveMovesProcessingPaymentToApprovedAndSetsPaidAt() {
        Payment payment = TestFixtures.processingPayment();

        payment.approve();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getFailureReason()).isNull();
        assertThat(payment.getPaidAt()).isNotNull();
    }

    @Test
    void refuseMovesPendingOrProcessingPaymentToRefused() {
        Payment pending = TestFixtures.pendingPayment();
        Payment processing = TestFixtures.processingPayment();

        pending.refuse("No limit");
        processing.refuse("Issuer declined");

        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.REFUSED);
        assertThat(pending.getFailureReason()).isEqualTo("No limit");
        assertThat(processing.getStatus()).isEqualTo(PaymentStatus.REFUSED);
        assertThat(processing.getFailureReason()).isEqualTo("Issuer declined");
        assertThat(processing.getPaidAt()).isNull();
    }

    @Test
    void cancelMovesPendingOrProcessingPaymentToCanceled() {
        Payment pending = TestFixtures.pendingPayment();
        Payment processing = TestFixtures.processingPayment();

        pending.cancel("Customer requested");
        processing.cancel(null);

        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(pending.getFailureReason()).isEqualTo("Customer requested");
        assertThat(processing.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(processing.getFailureReason()).isNull();
    }

    @Test
    void refundMovesApprovedPaymentToRefunded() {
        Payment payment = TestFixtures.approvedPayment();

        payment.refund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getPaidAt()).isEqualTo(TestFixtures.PAID_AT);
    }

    @Test
    void stateTransitionsRejectInvalidInputsAndStatuses() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TestFixtures.pendingPayment().process(" "))
                .withMessage("externalTransactionId must not be blank");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TestFixtures.pendingPayment().refuse(""))
                .withMessage("failureReason must not be blank");
        assertThatExceptionOfType(InvalidPaymentStatusException.class)
                .isThrownBy(() -> TestFixtures.approvedPayment().process("tx"))
                .withMessage("Cannot process payment from current status: APPROVED");
        assertThatExceptionOfType(InvalidPaymentStatusException.class)
                .isThrownBy(() -> TestFixtures.pendingPayment().approve())
                .withMessage("Cannot approve payment from current status: PENDING");
        assertThatExceptionOfType(InvalidPaymentStatusException.class)
                .isThrownBy(() -> TestFixtures.approvedPayment().refuse("No"))
                .withMessage("Cannot refuse payment from current status: APPROVED");
        assertThatExceptionOfType(InvalidPaymentStatusException.class)
                .isThrownBy(() -> TestFixtures.refundedPayment().cancel("No"))
                .withMessage("Cannot cancel payment from current status: REFUNDED");
        assertThatExceptionOfType(InvalidPaymentStatusException.class)
                .isThrownBy(() -> TestFixtures.processingPayment().refund())
                .withMessage("Cannot refund payment from current status: PROCESSING");
    }
}
