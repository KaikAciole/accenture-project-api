package br.com.accenture.customer.domain.event;

import java.time.Instant;
import java.util.UUID;

public record CustomerCreatedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID customerId,
        String email
) {

    public static final String TYPE = "customer.created";

    public static CustomerCreatedEvent of(UUID customerId, String email) {
        return new CustomerCreatedEvent(
                UUID.randomUUID(),
                TYPE,
                Instant.now(),
                customerId,
                email
        );
    }

}
