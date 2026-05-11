package br.com.accenture.customer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para atualização parcial de perfil do cliente. " +
        "Apenas os campos enviados serão modificados.")
public record UpdateProfileRequest(

        @Schema(description = "Nome completo do cliente",
                example = "Maria Silva",
                maxLength = 100)
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Schema(description = "CPF do cliente (apenas dígitos, exatamente 11). Imutável após a definição.",
                example = "12345678901",
                pattern = "\\d{11}")
        @Pattern(regexp = "\\d{11}", message = "cpf must contain exactly 11 digits")
        String cpf,

        @Schema(description = "Telefone do cliente (apenas dígitos, exatamente 11 com DDD)",
                example = "11987654321",
                pattern = "\\d{11}")
        @Pattern(regexp = "\\d{11}", message = "phone must contain exactly 11 digits")
        String phone

) {
}
