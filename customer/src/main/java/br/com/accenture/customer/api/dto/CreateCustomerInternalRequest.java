package br.com.accenture.customer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload interno para criação de cliente a partir do serviço de auth")
public record CreateCustomerInternalRequest(

        @Schema(description = "Email do cliente registrado no auth",
                example = "maria@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a well-formed email address")
        String email

) {
}
