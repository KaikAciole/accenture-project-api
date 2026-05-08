package br.com.accenture.customer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Payload para consulta de CEP")
public record CepLookupRequest(

        @Schema(description = "CEP a ser consultado (apenas dígitos, exatamente 8 caracteres)",
                example = "01001000",
                pattern = "\\d{8}",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "cep must not be blank")
        @Pattern(regexp = "\\d{8}", message = "cep must contain exactly 8 digits")
        String cep

) {
}
