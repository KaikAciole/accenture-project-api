package br.com.accenture.api_gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GatewayRegisterRequest(

        @NotBlank(message = "O e-mail não pode estar vazio")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "A senha não pode estar vazia")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String password
) {}