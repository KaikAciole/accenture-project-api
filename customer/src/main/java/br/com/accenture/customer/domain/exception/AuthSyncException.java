package br.com.accenture.customer.domain.exception;

public class AuthSyncException extends RuntimeException {

    public AuthSyncException(String message) {
        super(message);
    }

    public AuthSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
