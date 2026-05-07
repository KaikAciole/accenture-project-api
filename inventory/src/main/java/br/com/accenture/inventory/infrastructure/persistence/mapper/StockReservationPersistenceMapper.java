package br.com.accenture.inventory.infrastructure.persistence.mapper;

import br.com.accenture.inventory.domain.model.StockReservation;
import br.com.accenture.inventory.infrastructure.persistence.entity.StockReservationJpaEntity;

public final class StockReservationPersistenceMapper {

    private StockReservationPersistenceMapper() {
    }

    public static StockReservationJpaEntity toEntity(StockReservation stockReservation) {
        if (stockReservation == null) {
            return null;
        }

        return StockReservationJpaEntity.builder()
                .id(stockReservation.getId())
                .orderId(stockReservation.getOrderId())
                .product(ProductPersistenceMapper.toEntity(stockReservation.getProduct()))
                .reservedQuantity(stockReservation.getReservedQuantity())
                .status(stockReservation.getStatus())
                .build();
    }

    public static StockReservation toDomain(StockReservationJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return StockReservation.restore(
                entity.getId(),
                entity.getOrderId(),
                ProductPersistenceMapper.toDomain(entity.getProduct()),
                entity.getReservedQuantity(),
                entity.getStatus()
        );
    }
}