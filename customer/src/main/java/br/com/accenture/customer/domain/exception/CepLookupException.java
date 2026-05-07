package br.com.accenture.customer.domain.exception;

public class CepLookupException extends RuntimeException {

    public CepLookupException(String message, Throwable cause) {
        super(message, cause);
    }

}
