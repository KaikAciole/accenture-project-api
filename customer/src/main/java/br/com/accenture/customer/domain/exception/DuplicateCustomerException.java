package br.com.accenture.customer.domain.exception;

public class DuplicateCustomerException extends RuntimeException {

    public DuplicateCustomerException(String field, String value) {
        super("Customer already exists with " + field + ": " + value);
    }

}
