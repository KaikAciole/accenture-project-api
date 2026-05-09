package br.com.accenture.payment.api.mapper;

import br.com.accenture.payment.api.dto.request.PaymentRequest;
import br.com.accenture.payment.api.dto.response.PaymentResponse;
import br.com.accenture.payment.domain.model.Payment;

public final class PaymentDtoMapper {

    private PaymentDtoMapper() {
    }

    public static Payment toDomain(PaymentRequest request) {
        if (request == null) {
            return null;
        }

        return Payment.createNew(
                request.orderId(),
                request.customerId(),
                request.amount(),
                request.method()
        );
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