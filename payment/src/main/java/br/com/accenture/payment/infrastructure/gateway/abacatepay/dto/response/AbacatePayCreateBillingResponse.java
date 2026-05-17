package br.com.accenture.payment.infrastructure.gateway.abacatepay.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AbacatePayCreateBillingResponse(
        Data data,
        Boolean success
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String id,
            Integer amount,
            String status,
            Boolean devMode,
            String brCode,
            String brCodeBase64,
            String expiresAt
    ) {
    }
}
