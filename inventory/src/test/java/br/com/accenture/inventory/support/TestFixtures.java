package br.com.accenture.inventory.support;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.model.Product;
import br.com.accenture.inventory.domain.model.StockReservation;

import java.math.BigDecimal;
import java.util.UUID;

public final class TestFixtures {

    public static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID ORDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID RESERVATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private TestFixtures() {
    }

    public static Product newProduct() {
        return Product.createNew("SKU-001", "Notebook", "Electronics", BigDecimal.valueOf(1999.90), 10);
    }

    public static Product restoredProduct() {
        return Product.restore(PRODUCT_ID, "SKU-001", "Notebook", "Electronics", BigDecimal.valueOf(1999.90), 10, 0L);
    }

    public static StockReservation activeReservation() {
        return StockReservation.restore(RESERVATION_ID, ORDER_ID, restoredProduct(), 3, ReservationStatus.ACTIVE);
    }

    public static StockReservation confirmedReservation() {
        return StockReservation.restore(RESERVATION_ID, ORDER_ID, restoredProduct(), 3, ReservationStatus.CONFIRMED);
    }
}
