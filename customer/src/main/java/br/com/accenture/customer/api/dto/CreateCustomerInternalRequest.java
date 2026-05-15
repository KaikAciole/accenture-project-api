package br.com.accenture.customer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload interno para criação de cliente a partir do serviço de auth")
public record CreateCustomerInternalRequest(

        @Schema(description = "Nome completo do cliente",
                example = "Maria Silva",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "name must not be blank")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Schema(description = "Email do cliente registrado no auth",
                example = "maria@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a well-formed email address")
        String email,

        @Schema(description = "CPF do cliente (apenas dígitos, exatamente 11). Imutável após a criação.",
                example = "12345678901",
                pattern = "\\d{11}",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "cpf must not be blank")
        @Pattern(regexp = "\\d{11}", message = "cpf must contain exactly 11 digits")
        String cpf,

        @Schema(description = "Telefone do cliente (apenas dígitos, exatamente 11 com DDD)",
                example = "11987654321",
                pattern = "\\d{11}",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "phone must not be blank")
        @Pattern(regexp = "\\d{11}", message = "phone must contain exactly 11 digits")
        String phone

) {
}
