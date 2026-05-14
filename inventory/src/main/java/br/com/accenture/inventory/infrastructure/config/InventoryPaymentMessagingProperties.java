package br.com.accenture.inventory.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory.messaging.payment")
public record InventoryPaymentMessagingProperties(
        String exchange,
        Queue queue,
        RoutingKey routingKey
) {
    public record Queue(
            String approved,
            String refused,
            String canceled
    ) {
    }

    public record RoutingKey(
            String approved,
            String refused,
            String canceled
    ) {
    }
}