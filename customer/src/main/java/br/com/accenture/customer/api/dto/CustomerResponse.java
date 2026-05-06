package br.com.accenture.customer.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String email,
        String cpf,
        String phone,
        Instant createdAt,
        Instant updatedAt
) {
}
