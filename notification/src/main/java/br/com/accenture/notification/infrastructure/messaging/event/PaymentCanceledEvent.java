package br.com.accenture.notification.infrastructure.messaging.event;

import br.com.accenture.notification.domain.enums.PaymentMethod;

import java.math.BigDecimal;

public record PaymentCanceledEvent(
        String paymentId,
        String orderId,
        String customerId,
        BigDecimal amount,
        PaymentMethod method,
        String cancellationReason
) {
}
