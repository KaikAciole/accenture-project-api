package br.com.accenture.auth.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeEmailRequest(

        @NotNull(message = "Customer ID is required")
        UUID customerId,

        @NotBlank(message = "New email is required")
        @Email(message = "Invalid email format")
        String newEmail
) {}
