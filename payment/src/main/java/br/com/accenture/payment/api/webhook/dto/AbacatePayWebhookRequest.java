package br.com.accenture.payment.api.webhook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AbacatePayWebhookRequest(
        String id,
        String event,
        Integer apiVersion,
        Boolean devMode,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            Transparent transparent
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transparent(
            String id,
            String status,
            Integer amount,
            String externalId,
            Map<String, Object> metadata
    ) {
    }
}
