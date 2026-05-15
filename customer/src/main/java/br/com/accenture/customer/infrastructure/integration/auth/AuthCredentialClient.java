package br.com.accenture.customer.infrastructure.integration.auth;

import br.com.accenture.customer.domain.exception.AuthSyncException;
import br.com.accenture.customer.domain.exception.DuplicateEmailInAuthException;
import br.com.accenture.customer.domain.gateway.AuthCredentialGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

@Component
public class AuthCredentialClient implements AuthCredentialGateway {

    private final RestClient restClient;
    private final String internalSecret;

    public AuthCredentialClient(RestClient.Builder builder,
                                @Value("${integration.auth.base-url:http://localhost:8081}") String baseUrl,
                                @Value("${api.security.internal.secret:senha-secreta-microsservicos-1234}") String internalSecret) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalSecret = internalSecret;
    }

    @Override
    public void updateEmail(UUID customerId, String newEmail) {
        try {
            restClient.patch()
                    .uri("/internal/auth/credentials/email")
                    .header("X-Internal-Secret", internalSecret)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(Map.of("customerId", customerId, "newEmail", newEmail))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.value() == 409) {
                throw new DuplicateEmailInAuthException(newEmail);
            }
            throw new AuthSyncException("Auth service returned status " + status, ex);
        } catch (RestClientException ex) {
            throw new AuthSyncException("Failed to reach auth service", ex);
        }
    }
}
