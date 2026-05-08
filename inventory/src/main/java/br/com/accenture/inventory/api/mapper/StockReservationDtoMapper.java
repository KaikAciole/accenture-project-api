package br.com.accenture.inventory.api.mapper;

import br.com.accenture.inventory.api.dto.response.StockReservationResponse;
import br.com.accenture.inventory.domain.model.StockReservation;

public final class StockReservationDtoMapper {

    private StockReservationDtoMapper() {
    }

    public static StockReservationResponse toResponse(StockReservation stockReservation) {
        if (stockReservation == null) {
            return null;
        }

        return new StockReservationResponse(
                stockReservation.getId(),
                stockReservation.getOrderId(),
                stockReservation.getProduct().getId(),
                stockReservation.getProduct().getSku(),
                stockReservation.getReservedQuantity(),
                stockReservation.getStatus()
        );
    }
}