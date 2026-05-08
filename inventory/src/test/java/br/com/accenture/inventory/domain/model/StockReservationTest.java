package br.com.accenture.inventory.domain.model;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.exception.InvalidReservationStatusException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class StockReservationTest {

    @Test
    void createNewReservesStockAndStartsActive() {
        Product product = Product.createNew("SKU-001", "Notebook", "Electronics", BigDecimal.TEN, 10);
        UUID orderId = UUID.randomUUID();

        StockReservation reservation = StockReservation.createNew(orderId, product, 4);

        assertThat(reservation.getId()).isNull();
        assertThat(reservation.getOrderId()).isEqualTo(orderId);
        assertThat(reservation.getProduct()).isSameAs(product);
        assertThat(reservation.getReservedQuantity()).isEqualTo(4);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(product.getStockQuantity()).isEqualTo(6);
    }

    @Test
    void createNewRejectsInvalidValues() {
        Product product = Product.createNew("SKU-001", "Notebook", "Electronics", BigDecimal.TEN, 10);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> StockReservation.createNew(null, product, 1))
                .withMessage("orderId must not be null");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> StockReservation.createNew(UUID.randomUUID(), null, 1))
                .withMessage("product must not be null");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> StockReservation.createNew(UUID.randomUUID(), product, 0))
                .withMessage("reservedQuantity must be positive");
    }

    @Test
    void confirmChangesActiveReservationToConfirmed() {
        StockReservation reservation = StockReservation.createNew(
                UUID.randomUUID(),
                Product.createNew("SKU-001", "Notebook", "Electronics", BigDecimal.TEN, 10),
                4
        );

        reservation.confirm();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void cancelAndExpireReturnStock() {
        Product cancelProduct = Product.createNew("SKU-001", "Notebook", "Electronics", BigDecimal.TEN, 10);
        StockReservation canceled = StockReservation.createNew(UUID.randomUUID(), cancelProduct, 4);
        Product expireProduct = Product.createNew("SKU-002", "Mouse", "Electronics", BigDecimal.TEN, 10);
        StockReservation expired = StockReservation.createNew(UUID.randomUUID(), expireProduct, 3);

        canceled.cancel();
        expired.expire();

        assertThat(canceled.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(cancelProduct.getStockQuantity()).isEqualTo(10);
        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(expireProduct.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void statusTransitionsRequireActiveReservation() {
        StockReservation reservation = StockReservation.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Product.createNew("SKU-001", "Notebook", "Electronics", BigDecimal.TEN, 10),
                2,
                ReservationStatus.CONFIRMED
        );

        assertThatExceptionOfType(InvalidReservationStatusException.class)
                .isThrownBy(reservation::cancel)
                .withMessage("Cannot cancel reservation with status: CONFIRMED");
    }
}
