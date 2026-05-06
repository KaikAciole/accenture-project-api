package br.com.accenture.customer.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(

        @NotBlank(message = "name must not be blank")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a well-formed email address")
        String email,

        @NotBlank(message = "cpf must not be blank")
        @Pattern(regexp = "\\d{11}", message = "cpf must contain exactly 11 digits")
        String cpf,

        @NotBlank(message = "password must not be blank")
        @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
        String password,

        @NotBlank(message = "phone must not be blank")
        @Pattern(regexp = "\\d{11}", message = "phone must contain exactly 11 digits")
        String phone

) {
}
