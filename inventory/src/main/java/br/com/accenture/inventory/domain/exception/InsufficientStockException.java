package br.com.accenture.inventory.domain.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String sku, Integer requestedQuantity, Integer availableQuantity) {
        super("Insufficient stock for product sku: " + sku
                + ". Requested: " + requestedQuantity
                + ", available: " + availableQuantity);
    }
}