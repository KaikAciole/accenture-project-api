package br.com.accenture.customer.infrastructure.integration.viacep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ViaCepResponse(
        String cep,
        String logradouro,
        String complemento,
        String bairro,
        String localidade,
        String uf,
        Object erro
) {

    public boolean hasError() {
        return erro != null && "true".equalsIgnoreCase(String.valueOf(erro));
    }

}
