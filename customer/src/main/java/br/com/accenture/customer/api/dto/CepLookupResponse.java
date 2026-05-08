package br.com.accenture.customer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da consulta de CEP")
public record CepLookupResponse(

        @Schema(description = "CEP consultado (apenas dígitos)", example = "01001000")
        String zipCode,

        @Schema(description = "Logradouro retornado pelo ViaCEP", example = "Praça da Sé")
        String street,

        @Schema(description = "Complemento retornado pelo ViaCEP", example = "lado ímpar")
        String complement,

        @Schema(description = "Bairro", example = "Sé")
        String neighborhood,

        @Schema(description = "Cidade", example = "São Paulo")
        String city,

        @Schema(description = "Sigla da UF", example = "SP")
        String state
) {
}
