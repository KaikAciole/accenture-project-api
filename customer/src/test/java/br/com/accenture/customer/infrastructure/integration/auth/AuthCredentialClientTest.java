package br.com.accenture.customer.infrastructure.integration.auth;

import br.com.accenture.customer.domain.exception.AuthSyncException;
import br.com.accenture.customer.domain.exception.DuplicateEmailInAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthCredentialClientTest {

    private static final String BASE_URL = "http://localhost:8081";
    private static final String INTERNAL_SECRET = "test-secret";

    private MockRestServiceServer server;
    private AuthCredentialClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AuthCredentialClient(builder, BASE_URL, INTERNAL_SECRET);
    }

    @Test
    void updateEmail_shouldSendPatchWithSecretAndJsonBody() {
        UUID customerId = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/auth/credentials/email"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-Internal-Secret", INTERNAL_SECRET))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.newEmail").value("new@example.com"))
                .andRespond(withSuccess());

        assertThatCode(() -> client.updateEmail(customerId, "new@example.com"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void updateEmail_shouldThrowDuplicateEmailInAuthOn409() {
        UUID customerId = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/auth/credentials/email"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.updateEmail(customerId, "duplicated@example.com"))
                .isInstanceOf(DuplicateEmailInAuthException.class)
                .hasMessageContaining("duplicated@example.com");
    }

    @Test
    void updateEmail_shouldThrowAuthSyncExceptionOn5xx() {
        UUID customerId = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/auth/credentials/email"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.updateEmail(customerId, "new@example.com"))
                .isInstanceOf(AuthSyncException.class)
                .hasMessageContaining("Auth service returned status")
                .hasMessageContaining("500");
    }

    @Test
    void updateEmail_shouldThrowAuthSyncExceptionOnNetworkError() {
        UUID customerId = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/auth/credentials/email"))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> client.updateEmail(customerId, "new@example.com"))
                .isInstanceOf(AuthSyncException.class)
                .hasMessageContaining("Failed to reach auth service");
    }

}
