package br.com.accenture.payment.domain.wallet.exception;

public class InvalidTopUpRequestException extends RuntimeException {

    public InvalidTopUpRequestException(String message) {
        super(message);
    }

    public InvalidTopUpRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
