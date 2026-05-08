package br.com.accenture.customer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para criação ou atualização de um cliente")
public record CustomerRequest(

        @Schema(description = "Nome completo do cliente",
                example = "Maria Silva",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "name must not be blank")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Schema(description = "Endereço de e-mail válido e único",
                example = "maria.silva@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a well-formed email address")
        String email,

        @Schema(description = "CPF do cliente (apenas dígitos, exatamente 11 caracteres). Imutável após a criação.",
                example = "12345678901",
                pattern = "\\d{11}",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "cpf must not be blank")
        @Pattern(regexp = "\\d{11}", message = "cpf must contain exactly 11 digits")
        String cpf,

        @Schema(description = "Senha do cliente (8 a 100 caracteres)",
                example = "senhaSegura123",
                minLength = 8,
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "password must not be blank")
        @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
        String password,

        @Schema(description = "Telefone do cliente (apenas dígitos, exatamente 11 caracteres com DDD)",
                example = "11987654321",
                pattern = "\\d{11}",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "phone must not be blank")
        @Pattern(regexp = "\\d{11}", message = "phone must contain exactly 11 digits")
        String phone

) {
}
