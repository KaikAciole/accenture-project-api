package br.com.accenture.payment.application.port;

import br.com.accenture.payment.domain.payment.model.Payment;

public interface PaymentEventPublisher {

    void publishPaymentApproved(Payment payment);

    void publishPaymentRefused(Payment payment);

    void publishPaymentCanceled(Payment payment);

    void publishPaymentRefunded(Payment payment, String reason);
}
