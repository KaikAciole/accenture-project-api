package br.com.accenture.order.application.dto.event;

import java.util.UUID;

public record OrderReservedEvent(
        UUID orderId,
        UUID customerId
) {}
