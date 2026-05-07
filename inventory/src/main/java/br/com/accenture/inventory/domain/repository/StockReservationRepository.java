package br.com.accenture.inventory.domain.repository;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.model.StockReservation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository {

    StockReservation save(StockReservation stockReservation);

    Optional<StockReservation> findById(UUID id);

    List<StockReservation> findByOrderId(UUID orderId);

    List<StockReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    List<StockReservation> findByProductId(UUID productId);

    List<StockReservation> findAll();

    void deleteById(UUID id);
}