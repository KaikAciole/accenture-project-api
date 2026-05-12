package br.com.accenture.payment.api.mapper;

import br.com.accenture.payment.api.dto.request.PaymentRequest;
import br.com.accenture.payment.domain.payment.enums.PaymentMethod;
import br.com.accenture.payment.domain.payment.enums.PaymentStatus;
import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDtoMapperTest {

    @Test
    void toDomainMapsRequestToNewPayment() {
        PaymentRequest request = new PaymentRequest(
                TestFixtures.ORDER_ID,
                TestFixtures.CUSTOMER_ID,
                TestFixtures.AMOUNT,
                PaymentMethod.DEBIT_CARD
        );

        Payment payment = PaymentDtoMapper.toDomain(request);

        assertThat(payment.getOrderId()).isEqualTo(TestFixtures.ORDER_ID);
        assertThat(payment.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(payment.getAmount()).isEqualByComparingTo(TestFixtures.AMOUNT);
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.DEBIT_CARD);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void toResponseMapsAllDomainFields() {
        Payment payment = TestFixtures.approvedPayment();

        var response = PaymentDtoMapper.toResponse(payment);

        assertThat(response.id()).isEqualTo(TestFixtures.PAYMENT_ID);
        assertThat(response.orderId()).isEqualTo(TestFixtures.ORDER_ID);
        assertThat(response.customerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(response.amount()).isEqualByComparingTo(TestFixtures.AMOUNT);
        assertThat(response.method()).isEqualTo(PaymentMethod.PIX);
        assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.externalTransactionId()).isEqualTo(TestFixtures.EXTERNAL_TRANSACTION_ID);
        assertThat(response.failureReason()).isNull();
        assertThat(response.paidAt()).isEqualTo(TestFixtures.PAID_AT);
        assertThat(response.createdAt()).isEqualTo(TestFixtures.CREATED_AT);
        assertThat(response.updatedAt()).isEqualTo(TestFixtures.UPDATED_AT);
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(PaymentDtoMapper.toDomain(null)).isNull();
        assertThat(PaymentDtoMapper.toResponse(null)).isNull();
    }
}
