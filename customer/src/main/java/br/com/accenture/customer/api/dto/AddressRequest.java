package br.com.accenture.customer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para criação ou atualização de um endereço")
public record AddressRequest(

        @Schema(description = "Logradouro (rua, avenida, etc.)",
                example = "Rua das Flores",
                maxLength = 150,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "street must not be blank")
        @Size(max = 150, message = "street must be at most 150 characters")
        String street,

        @Schema(description = "Número do imóvel",
                example = "123",
                maxLength = 20,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "number must not be blank")
        @Size(max = 20, message = "number must be at most 20 characters")
        String number,

        @Schema(description = "Complemento do endereço (opcional)",
                example = "Apto 42",
                maxLength = 100)
        @Size(max = 100, message = "complement must be at most 100 characters")
        String complement,

        @Schema(description = "Bairro",
                example = "Centro",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "neighborhood must not be blank")
        @Size(max = 100, message = "neighborhood must be at most 100 characters")
        String neighborhood,

        @Schema(description = "Cidade",
                example = "São Paulo",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "city must not be blank")
        @Size(max = 100, message = "city must be at most 100 characters")
        String city,

        @Schema(description = "Sigla da unidade federativa (UF), 2 letras maiúsculas",
                example = "SP",
                pattern = "[A-Z]{2}",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "state must not be blank")
        @Pattern(regexp = "[A-Z]{2}", message = "state must be exactly 2 uppercase letters")
        String state,

        @Schema(description = "CEP (apenas dígitos, exatamente 8 caracteres)",
                example = "01001000",
                pattern = "\\d{8}",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "zipCode must not be blank")
        @Pattern(regexp = "\\d{8}", message = "zipCode must contain exactly 8 digits")
        String zipCode

) {
}
