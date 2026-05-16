package br.com.accenture.customer.domain.exception;

public class DuplicateEmailInAuthException extends RuntimeException {

    public DuplicateEmailInAuthException(String email) {
        super("Email already in use by another account: " + email);
    }
}
