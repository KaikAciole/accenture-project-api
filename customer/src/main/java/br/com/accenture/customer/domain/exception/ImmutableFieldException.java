package br.com.accenture.customer.domain.exception;

public class ImmutableFieldException extends RuntimeException {

    public ImmutableFieldException(String field) {
        super("Field '" + field + "' cannot be changed after creation");
    }

}
