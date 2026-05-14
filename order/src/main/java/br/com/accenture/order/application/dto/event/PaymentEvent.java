package br.com.accenture.order.application.dto.event;

import java.util.UUID;

public record PaymentEvent(
        UUID eventId,
        UUID paymentId,
        UUID orderId,
        String method,
        String failureReason,
        String cancellationReason
) {}