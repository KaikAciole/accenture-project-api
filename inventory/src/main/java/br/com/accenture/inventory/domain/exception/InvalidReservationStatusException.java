package br.com.accenture.inventory.domain.exception;

import br.com.accenture.inventory.domain.enums.ReservationStatus;

public class InvalidReservationStatusException extends RuntimeException {

    public InvalidReservationStatusException(ReservationStatus currentStatus, String action) {
        super("Cannot " + action + " reservation with status: " + currentStatus);
    }
}