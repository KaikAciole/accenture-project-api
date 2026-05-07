package br.com.accenture.customer.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CepLookupRequest(

        @NotBlank(message = "cep must not be blank")
        @Pattern(regexp = "\\d{8}", message = "cep must contain exactly 8 digits")
        String cep

) {
}
