package br.com.accenture.customer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Representação de um endereço retornada pela API")
public record AddressResponse(

        @Schema(description = "Identificador único do endereço",
                example = "1d2e8400-e29b-41d4-a716-446655441111",
                format = "uuid")
        UUID id,

        @Schema(description = "Identificador do cliente dono do endereço",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid")
        UUID customerId,

        @Schema(description = "Logradouro", example = "Rua das Flores")
        String street,

        @Schema(description = "Número do imóvel", example = "123")
        String number,

        @Schema(description = "Complemento", example = "Apto 42")
        String complement,

        @Schema(description = "Bairro", example = "Centro")
        String neighborhood,

        @Schema(description = "Cidade", example = "São Paulo")
        String city,

        @Schema(description = "Sigla da UF", example = "SP")
        String state,

        @Schema(description = "CEP (apenas dígitos)", example = "01001000")
        String zipCode,

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
