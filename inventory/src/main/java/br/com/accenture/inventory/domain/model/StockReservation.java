package br.com.accenture.inventory.domain.model;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.exception.InvalidReservationStatusException;
import lombok.Getter;

import java.util.UUID;

@Getter
public class StockReservation {

    private UUID id;
    private UUID orderId;
    private Product product;
    private Integer reservedQuantity;
    private ReservationStatus status;

    private StockReservation(UUID id,
                             UUID orderId,
                             Product product,
                             Integer reservedQuantity,
                             ReservationStatus status) {
        this.id = id;
        this.orderId = orderId;
        this.product = product;
        this.reservedQuantity = reservedQuantity;
        this.status = status;
    }

    public static StockReservation createNew(UUID orderId,
                                             Product product,
                                             Integer reservedQuantity) {
        requireNotNull(orderId, "orderId");
        requireNotNull(product, "product");
        requirePositive(reservedQuantity, "reservedQuantity");

        product.decreaseStock(reservedQuantity);

        return new StockReservation(
                null,
                orderId,
                product,
                reservedQuantity,
                ReservationStatus.ACTIVE
        );
    }

    public static StockReservation restore(UUID id,
                                           UUID orderId,
                                           Product product,
                                           Integer reservedQuantity,
                                           ReservationStatus status) {
        requireNotNull(id, "id");
        requireNotNull(orderId, "orderId");
        requireNotNull(product, "product");
        requirePositive(reservedQuantity, "reservedQuantity");
        requireNotNull(status, "status");

        return new StockReservation(id, orderId, product, reservedQuantity, status);
    }

    public void confirm() {
        validateActive("confirm");

        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        validateActive("cancel");

        this.product.increaseStock(this.reservedQuantity);
        this.status = ReservationStatus.CANCELED;
    }

    public void release() {
        if (this.status != ReservationStatus.ACTIVE && this.status != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStatusException(this.status, "release");
        }

        this.product.increaseStock(this.reservedQuantity);
        this.status = ReservationStatus.CANCELED;
    }

    public void expire() {
        validateActive("expire");

        this.product.increaseStock(this.reservedQuantity);
        this.status = ReservationStatus.EXPIRED;
    }

    private void validateActive(String action) {
        if (this.status != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStatusException(this.status, action);
        }
    }

    private static void requireNotNull(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
    }

    private static void requirePositive(Integer value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }
}