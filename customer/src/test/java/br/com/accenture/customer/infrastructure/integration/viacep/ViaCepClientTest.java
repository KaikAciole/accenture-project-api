package br.com.accenture.customer.infrastructure.integration.viacep;

import br.com.accenture.customer.domain.exception.CepLookupException;
import br.com.accenture.customer.domain.exception.CepNotFoundException;
import br.com.accenture.customer.domain.model.CepLookupResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ViaCepClientTest {

    private static final String BASE_URL = "https://viacep.com.br/ws";

    private MockRestServiceServer server;
    private ViaCepClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ViaCepClient(builder, BASE_URL);
    }

    @Test
    void lookup_shouldMapSuccessResponse() {
        server.expect(requestTo(BASE_URL + "/01001000/json/"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "cep": "01001-000",
                          "logradouro": "Praça da Sé",
                          "complemento": "lado ímpar",
                          "bairro": "Sé",
                          "localidade": "São Paulo",
                          "uf": "SP"
                        }
                        """, MediaType.APPLICATION_JSON));

        CepLookupResult result = client.lookup("01001000");

        assertThat(result.zipCode()).isEqualTo("01001000");
        assertThat(result.street()).isEqualTo("Praça da Sé");
        assertThat(result.complement()).isEqualTo("lado ímpar");
        assertThat(result.neighborhood()).isEqualTo("Sé");
        assertThat(result.city()).isEqualTo("São Paulo");
        assertThat(result.state()).isEqualTo("SP");
        server.verify();
    }

    @Test
    void lookup_shouldFallBackToInputCepWhenResponseHasNoCep() {
        server.expect(requestTo(BASE_URL + "/01001000/json/"))
                .andRespond(withSuccess("""
                        {
                          "logradouro": "Praça da Sé",
                          "bairro": "Sé",
                          "localidade": "São Paulo",
                          "uf": "SP"
                        }
                        """, MediaType.APPLICATION_JSON));

        CepLookupResult result = client.lookup("01001000");

        assertThat(result.zipCode()).isEqualTo("01001000");
    }

    @Test
    void lookup_shouldThrowCepNotFoundWhenViaCepReturnsErrorTrue() {
        server.expect(requestTo(BASE_URL + "/99999999/json/"))
                .andRespond(withSuccess("""
                        { "erro": "true" }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.lookup("99999999"))
                .isInstanceOf(CepNotFoundException.class)
                .hasMessageContaining("99999999");
    }

    @Test
    void lookup_shouldThrowCepNotFoundOn400() {
        server.expect(requestTo(BASE_URL + "/abc/json/"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.lookup("abc"))
                .isInstanceOf(CepNotFoundException.class)
                .hasMessageContaining("abc");
    }

    @Test
    void lookup_shouldThrowCepNotFoundOn404() {
        server.expect(requestTo(BASE_URL + "/00000000/json/"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.lookup("00000000"))
                .isInstanceOf(CepNotFoundException.class);
    }

    @Test
    void lookup_shouldThrowCepLookupExceptionOn5xx() {
        server.expect(requestTo(BASE_URL + "/01001000/json/"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.lookup("01001000"))
                .isInstanceOf(CepLookupException.class)
                .hasMessageContaining("ViaCEP returned status");
    }

    @Test
    void lookup_shouldThrowCepLookupExceptionOnNetworkError() {
        server.expect(requestTo(BASE_URL + "/01001000/json/"))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> client.lookup("01001000"))
                .isInstanceOf(CepLookupException.class)
                .hasMessageContaining("Failed to reach ViaCEP");
    }

}
