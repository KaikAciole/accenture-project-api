package br.com.accenture.inventory.domain.repository;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.model.StockReservation;
import br.com.accenture.inventory.domain.pagination.PageRequest;
import br.com.accenture.inventory.domain.pagination.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository {

    StockReservation save(StockReservation stockReservation);

    Optional<StockReservation> findById(UUID id);

    Optional<StockReservation> findByOrderIdAndProductId(UUID orderId, UUID productId);

    PageResult<StockReservation> findByOrderId(UUID orderId, PageRequest pageRequest);

    PageResult<StockReservation> findByOrderIdAndStatus(UUID orderId,
                                                        ReservationStatus status,
                                                        PageRequest pageRequest);

    PageResult<StockReservation> findByProductId(UUID productId, PageRequest pageRequest);

    PageResult<StockReservation> findAll(PageRequest pageRequest);

    List<StockReservation> findAllByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    void deleteById(UUID id);
}
