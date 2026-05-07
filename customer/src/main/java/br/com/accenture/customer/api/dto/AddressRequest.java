package br.com.accenture.customer.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(

        @NotBlank(message = "street must not be blank")
        @Size(max = 150, message = "street must be at most 150 characters")
        String street,

        @NotBlank(message = "number must not be blank")
        @Size(max = 20, message = "number must be at most 20 characters")
        String number,

        @Size(max = 100, message = "complement must be at most 100 characters")
        String complement,

        @NotBlank(message = "neighborhood must not be blank")
        @Size(max = 100, message = "neighborhood must be at most 100 characters")
        String neighborhood,

        @NotBlank(message = "city must not be blank")
        @Size(max = 100, message = "city must be at most 100 characters")
        String city,

        @NotBlank(message = "state must not be blank")
        @Pattern(regexp = "[A-Z]{2}", message = "state must be exactly 2 uppercase letters")
        String state,

        @NotBlank(message = "zipCode must not be blank")
        @Pattern(regexp = "\\d{8}", message = "zipCode must contain exactly 8 digits")
        String zipCode

) {
}
