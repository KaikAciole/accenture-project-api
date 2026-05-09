package br.com.accenture.payment.domain.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID id) {
        super("Payment not found with id: " + id);
    }

    public static PaymentNotFoundException byOrderId(UUID orderId) {
        return new PaymentNotFoundException("Payment not found with order id: " + orderId);
    }

    private PaymentNotFoundException(String message) {
        super(message);
    }

}



