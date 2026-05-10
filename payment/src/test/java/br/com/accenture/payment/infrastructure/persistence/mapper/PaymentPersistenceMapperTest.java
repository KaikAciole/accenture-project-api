package br.com.accenture.payment.infrastructure.persistence.mapper;

import br.com.accenture.payment.domain.enums.PaymentMethod;
import br.com.accenture.payment.domain.enums.PaymentStatus;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPersistenceMapperTest {

    @Test
    void toEntityMapsAllDomainFields() {
        var entity = PaymentPersistenceMapper.toEntity(TestFixtures.approvedPayment());

        assertThat(entity.getId()).isEqualTo(TestFixtures.PAYMENT_ID);
        assertThat(entity.getOrderId()).isEqualTo(TestFixtures.ORDER_ID);
        assertThat(entity.getCustomerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
        assertThat(entity.getAmount()).isEqualByComparingTo(TestFixtures.AMOUNT);
        assertThat(entity.getMethod()).isEqualTo(PaymentMethod.PIX);
        assertThat(entity.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(entity.getExternalTransactionId()).isEqualTo(TestFixtures.EXTERNAL_TRANSACTION_ID);
        assertThat(entity.getPaidAt()).isEqualTo(TestFixtures.PAID_AT);
        assertThat(entity.getCreatedAt()).isEqualTo(TestFixtures.CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(TestFixtures.UPDATED_AT);
        assertThat(entity.getVersion()).isEqualTo(1L);
    }

    @Test
    void toDomainMapsAllEntityFields() {
        var domain = PaymentPersistenceMapper.toDomain(PaymentPersistenceMapper.toEntity(TestFixtures.refusedPayment()));

        assertThat(domain.getId()).isEqualTo(TestFixtures.PAYMENT_ID);
        assertThat(domain.getStatus()).isEqualTo(PaymentStatus.REFUSED);
        assertThat(domain.getFailureReason()).isEqualTo(TestFixtures.FAILURE_REASON);
        assertThat(domain.getCreatedAt()).isEqualTo(TestFixtures.CREATED_AT);
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(PaymentPersistenceMapper.toEntity(null)).isNull();
        assertThat(PaymentPersistenceMapper.toDomain(null)).isNull();
    }
}
