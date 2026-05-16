package br.com.accenture.customer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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

        @Schema(description = "Email do cliente. Quando alterado, é sincronizado com o serviço de auth.",
                example = "maria.silva@example.com",
                maxLength = 100)
        @Email(message = "email must be a valid email address")
        @Size(max = 100, message = "email must be at most 100 characters")
        String email,

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
