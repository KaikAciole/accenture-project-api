package br.com.accenture.customer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Representação de um cliente retornada pela API")
public record CustomerResponse(

        @Schema(description = "Identificador único do cliente",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid")
        UUID id,

        @Schema(description = "Nome completo do cliente", example = "Maria Silva")
        String name,

        @Schema(description = "E-mail do cliente", example = "maria.silva@example.com")
        String email,

        @Schema(description = "CPF do cliente (apenas dígitos)", example = "12345678901")
        String cpf,

        @Schema(description = "Telefone do cliente com DDD", example = "11987654321")
        String phone,

        @Schema(description = "Data e hora de criação do registro",
                example = "2026-05-07T14:30:00Z",
                format = "date-time")
        Instant createdAt,

        @Schema(description = "Data e hora da última atualização do registro",
                example = "2026-05-07T14:30:00Z",
                format = "date-time")
        Instant updatedAt
) {
}
