package br.com.accenture.inventory.application.port;

import br.com.accenture.inventory.domain.model.StockReservation;

import java.util.UUID;

public interface StockEventPublisher {

    void publishStockReserved(StockReservation reservation);

    void publishStockReservationConfirmed(StockReservation reservation);

    void publishStockReservationCanceled(StockReservation reservation);

    void publishStockReservationFailed(UUID orderId, String sku, Integer quantity, String reason);
}