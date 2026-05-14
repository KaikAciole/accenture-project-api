package br.com.accenture.inventory.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory.messaging.order")
public record InventoryOrderMessagingProperties(
        String exchange,
        Queue queue,
        RoutingKey routingKey
) {
    public record Queue(
            String created,
            String canceled
    ) {
    }

    public record RoutingKey(
            String created,
            String canceled
    ) {
    }
}