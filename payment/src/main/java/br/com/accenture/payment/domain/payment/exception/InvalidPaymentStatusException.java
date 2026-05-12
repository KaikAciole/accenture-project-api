package br.com.accenture.payment.domain.payment.exception;

import br.com.accenture.payment.domain.payment.enums.PaymentStatus;

public class InvalidPaymentStatusException extends RuntimeException {

    public InvalidPaymentStatusException(PaymentStatus currentStatus, String action) {
        super("Cannot " + action + " payment from current status: " + currentStatus);
    }
}