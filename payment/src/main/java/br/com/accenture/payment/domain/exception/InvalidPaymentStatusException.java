package br.com.accenture.payment.domain.exception;

import br.com.accenture.payment.domain.enums.PaymentStatus;

public class InvalidPaymentStatusException extends RuntimeException {

    public InvalidPaymentStatusException(PaymentStatus currentStatus, String action) {
        super("Cannot " + action + " payment from current status: " + currentStatus);
    }
}