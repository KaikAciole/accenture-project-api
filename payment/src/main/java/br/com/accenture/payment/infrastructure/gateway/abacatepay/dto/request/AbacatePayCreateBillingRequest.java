package br.com.accenture.payment.infrastructure.gateway.abacatepay.dto.request;

import java.util.Map;

public record AbacatePayCreateBillingRequest(
        String method,
        Data data
) {
    public record Data(
            Integer amount,
            String description,
            String externalId,
            Map<String, Object> metadata
    ) {
    }
}
