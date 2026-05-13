package br.com.accenture.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.messaging")
public record PaymentMessagingProperties(
        String exchange,
        RoutingKey routingKey
) {
    public record RoutingKey(
            String approved,
            String refused,
            String canceled
    ) {}
}
