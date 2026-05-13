package br.com.accenture.order.application.dto.event;

import java.util.UUID;

public record OrderPaidEvent(
        UUID orderId,
        String customerId
) {}