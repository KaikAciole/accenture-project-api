package br.com.accenture.order.application.dto.event;

import java.util.UUID;

public record OrderCanceledEvent(
        UUID orderId,
        UUID customerId,
        String reason
) {}