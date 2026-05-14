package br.com.accenture.notification.infrastructure.customer;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestClientCustomerLookupAdapterTest {

    private static final String BASE_URL = "http://customer-test";

    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final RestClient restClient = builder.build();
    private final RestClientCustomerLookupAdapter adapter = new RestClientCustomerLookupAdapter(restClient);

    @Test
    void findEmailByCustomerIdReturnsEmailFromCustomerService() {
        server.expect(requestTo(BASE_URL + "/internal/customers/customer-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"customer-1","email":"user@example.com"}
                        """, MediaType.APPLICATION_JSON));

        Optional<String> result = adapter.findEmailByCustomerId("customer-1");

        assertThat(result).contains("user@example.com");
        server.verify();
    }

    @Test
    void findEmailByCustomerIdReturnsEmptyWhenCustomerNotFound() {
        server.expect(requestTo(BASE_URL + "/internal/customers/missing"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<String> result = adapter.findEmailByCustomerId("missing");

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void findEmailByCustomerIdReturnsEmptyOnServerError() {
        server.expect(requestTo(BASE_URL + "/internal/customers/error"))
                .andRespond(withServerError());

        Optional<String> result = adapter.findEmailByCustomerId("error");

        assertThat(result).isEmpty();
        server.verify();
    }
}
