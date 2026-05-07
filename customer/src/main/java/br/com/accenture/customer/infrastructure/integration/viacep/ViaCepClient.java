package br.com.accenture.customer.infrastructure.integration.viacep;

import br.com.accenture.customer.domain.exception.CepLookupException;
import br.com.accenture.customer.domain.exception.CepNotFoundException;
import br.com.accenture.customer.domain.gateway.CepLookupGateway;
import br.com.accenture.customer.domain.model.CepLookupResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ViaCepClient implements CepLookupGateway {

    private final RestClient restClient;

    public ViaCepClient(RestClient.Builder builder,
                        @Value("${integration.viacep.base-url:https://viacep.com.br/ws}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public CepLookupResult lookup(String cep) {
        ViaCepResponse response;
        try {
            response = restClient.get()
                    .uri("/{cep}/json/", cep)
                    .retrieve()
                    .body(ViaCepResponse.class);
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.value() == 400 || status.value() == 404) {
                throw new CepNotFoundException(cep);
            }
            throw new CepLookupException("ViaCEP returned status " + status, ex);
        } catch (RestClientException ex) {
            throw new CepLookupException("Failed to reach ViaCEP", ex);
        }

        if (response == null || response.hasError()) {
            throw new CepNotFoundException(cep);
        }

        return toDomain(response, cep);
    }

    private CepLookupResult toDomain(ViaCepResponse response, String fallbackCep) {
        String zipCode = response.cep() != null
                ? response.cep().replace("-", "")
                : fallbackCep;
        return new CepLookupResult(
                zipCode,
                response.logradouro(),
                response.complemento(),
                response.bairro(),
                response.localidade(),
                response.uf()
        );
    }

}
