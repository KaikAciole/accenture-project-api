package br.com.accenture.customer.domain.exception;

import java.util.UUID;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(UUID id) {
        super("Address not found with id: " + id);
    }

    public AddressNotFoundException(String message) {
        super(message);
    }

}
