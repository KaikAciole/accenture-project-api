package br.com.accenture.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.messaging.order")
public record PaymentOrderMessagingProperties(
        String exchange,
        Queue queue,
        RoutingKey routingKey
) {
    public record Queue(
            String canceled
    ) {
    }

    public record RoutingKey(
            String canceled
    ) {
    }
}