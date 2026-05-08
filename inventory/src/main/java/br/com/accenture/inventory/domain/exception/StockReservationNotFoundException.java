package br.com.accenture.inventory.domain.exception;

import java.util.UUID;

public class StockReservationNotFoundException extends RuntimeException {

    public StockReservationNotFoundException(UUID id) {
        super("Stock reservation not found with id: " + id);
    }
}