package br.com.accenture.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.messaging.customer")
public record PaymentCustomerMessagingProperties(
        String exchange,
        Queue queue,
        RoutingKey routingKey
) {
    public record Queue(
            String created
    ) {
    }

    public record RoutingKey(
            String created
    ) {
    }
}