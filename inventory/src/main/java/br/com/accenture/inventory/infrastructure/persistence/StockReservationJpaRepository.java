package br.com.accenture.inventory.infrastructure.persistence;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.infrastructure.persistence.entity.StockReservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockReservationJpaRepository extends JpaRepository<StockReservationJpaEntity, UUID> {

    List<StockReservationJpaEntity> findByOrderId(UUID orderId);

    List<StockReservationJpaEntity> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    List<StockReservationJpaEntity> findByProductId(UUID productId);
}