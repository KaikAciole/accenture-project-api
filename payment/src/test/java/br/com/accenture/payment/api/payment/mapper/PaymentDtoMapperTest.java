package br.com.accenture.payment.api.payment.mapper;

import br.com.accenture.payment.domain.payment.enums.PaymentMethod;
import br.com.accenture.payment.domain.payment.enums.PaymentStatus;
import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDtoMapperTest {

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
        assertThat(PaymentDtoMapper.toResponse(null)).isNull();
    }
}
