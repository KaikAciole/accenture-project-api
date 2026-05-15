package br.com.accenture.api_gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GatewayRegisterRequest(

        @NotBlank(message = "O nome não pode ser vazio")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,

        @NotBlank(message = "O email não pode ser vazio")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "A senha não pode ser vazia")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String password,

        @NotBlank(message = "O CPF não pode ser vazio")
        @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos numéricos")
        String cpf,

        @NotBlank(message = "O telefone não pode ser vazio")
        @Pattern(regexp = "\\d{11}", message = "O telefone deve conter exatamente 11 dígitos (com DDD)")
        String phone

) {}