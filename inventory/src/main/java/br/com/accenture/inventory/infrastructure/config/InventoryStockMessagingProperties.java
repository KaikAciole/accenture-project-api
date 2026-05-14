package br.com.accenture.inventory.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory.messaging.stock")
public record InventoryStockMessagingProperties(
        String exchange,
        RoutingKey routingKey
) {
    public record RoutingKey(
            String reserved,
            String reservationFailed,
            String reservationConfirmed,
            String reservationCanceled
    ) {
    }
}