package br.com.accenture.inventory.domain.exception;

public class DuplicateProductException extends RuntimeException {

    public DuplicateProductException(String field, String value) {
        super("Product already exists with " + field + ": " + value);
    }
}