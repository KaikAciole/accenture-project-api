package br.com.accenture.api_gateway.dto;

public record GatewayRegisterRequest(
        String email,
        String password
) {}