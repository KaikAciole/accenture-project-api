package br.com.accenture.payment.api.payment.mapper;

import br.com.accenture.payment.api.payment.dto.response.PaymentResponse;
import br.com.accenture.payment.domain.payment.model.Payment;

public final class PaymentDtoMapper {

    private PaymentDtoMapper() {
    }

    public static PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getExternalTransactionId(),
                payment.getFailureReason(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}