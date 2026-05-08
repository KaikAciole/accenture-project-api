package br.com.accenture.inventory.api.dto.response;

import br.com.accenture.inventory.domain.enums.ReservationStatus;

import java.util.UUID;

public record StockReservationResponse(
        UUID id,
        UUID orderId,
        UUID productId,
        String productSku,
        Integer reservedQuantity,
        ReservationStatus status
) {
}