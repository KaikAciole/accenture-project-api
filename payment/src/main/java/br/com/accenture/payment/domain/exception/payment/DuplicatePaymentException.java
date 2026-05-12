package br.com.accenture.payment.domain.exception.payment;

import java.util.UUID;

public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(UUID orderId) {
        super("Payment already exists for order id: " + orderId);
    }

}
