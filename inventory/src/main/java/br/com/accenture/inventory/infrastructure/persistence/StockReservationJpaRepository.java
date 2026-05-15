package br.com.accenture.inventory.infrastructure.persistence;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.infrastructure.persistence.entity.StockReservationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockReservationJpaRepository extends JpaRepository<StockReservationJpaEntity, UUID> {

    Optional<StockReservationJpaEntity> findByOrderIdAndProduct_Id(UUID orderId, UUID productId);

    Page<StockReservationJpaEntity> findByOrderId(UUID orderId, Pageable pageable);

    Page<StockReservationJpaEntity> findByOrderIdAndStatus(UUID orderId,
                                                           ReservationStatus status,
                                                           Pageable pageable);

    Page<StockReservationJpaEntity> findByProductId(UUID productId, Pageable pageable);

    List<StockReservationJpaEntity> findAllByOrderIdAndStatus(UUID orderId,
                                                              ReservationStatus status);
}
