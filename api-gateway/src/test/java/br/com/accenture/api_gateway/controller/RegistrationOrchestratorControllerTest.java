package br.com.accenture.api_gateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationOrchestratorController.class)
class RegistrationOrchestratorControllerTest {

    private static final String VALID_BODY = """
            {
              "name": "John Doe",
              "email": "john@example.com",
              "password": "secret123",
              "cpf": "12345678901",
              "phone": "11912345678"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebClient.Builder webClientBuilder;

    private void wireExchange(Function<org.springframework.web.reactive.function.client.ClientRequest, Mono<ClientResponse>> dispatcher) {
        ExchangeFunction exchange = dispatcher::apply;
        WebClient client = WebClient.builder().exchangeFunction(exchange).build();
        when(webClientBuilder.build()).thenReturn(client);
    }

    @BeforeEach
    void resetBuilder() {
        // each test wires its own exchange function
    }

    @Test
    void shouldRegisterCustomerAndAuthSuccessfully() throws Exception {
        UUID generatedId = UUID.randomUUID();
        AtomicInteger customerCalls = new AtomicInteger();
        AtomicInteger authCalls = new AtomicInteger();

        wireExchange(req -> {
            String url = req.url().toString();
            if (url.endsWith("/internal/customers")) {
                customerCalls.incrementAndGet();
                return Mono.just(ClientResponse.create(HttpStatus.CREATED)
                        .header("Content-Type", "application/json")
                        .body("{\"id\":\"" + generatedId + "\"}").build());
            }
            if (url.endsWith("/api/v1/auth/register")) {
                authCalls.incrementAndGet();
                return Mono.just(ClientResponse.create(HttpStatus.CREATED).build());
            }
            return Mono.error(new IllegalStateException("Unexpected URL: " + url));
        });

        MvcResult result = mockMvc.perform(post("/api/v1/gateway/register-flow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isCreated());

        assertThat(customerCalls.get()).isEqualTo(1);
        assertThat(authCalls.get()).isEqualTo(1);
    }

    @Test
    void shouldReturnConflictWhenCustomerAlreadyExists() throws Exception {
        wireExchange(req -> {
            String url = req.url().toString();
            if (url.endsWith("/internal/customers")) {
                return Mono.just(ClientResponse.create(HttpStatus.CONFLICT)
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"duplicate\"}").build());
            }
            return Mono.error(new IllegalStateException("Unexpected URL: " + url));
        });

        MvcResult result = mockMvc.perform(post("/api/v1/gateway/register-flow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRollbackCustomerWhenAuthFails() throws Exception {
        UUID generatedId = UUID.randomUUID();
        AtomicInteger customerCreates = new AtomicInteger();
        AtomicInteger authAttempts = new AtomicInteger();
        AtomicInteger customerDeletes = new AtomicInteger();

        wireExchange(req -> {
            String url = req.url().toString();
            String method = req.method().name();
            if ("POST".equals(method) && url.endsWith("/internal/customers")) {
                customerCreates.incrementAndGet();
                return Mono.just(ClientResponse.create(HttpStatus.CREATED)
                        .header("Content-Type", "application/json")
                        .body("{\"id\":\"" + generatedId + "\"}").build());
            }
            if ("POST".equals(method) && url.endsWith("/api/v1/auth/register")) {
                authAttempts.incrementAndGet();
                return Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
            if ("DELETE".equals(method) && url.contains("/customers/" + generatedId)) {
                customerDeletes.incrementAndGet();
                return Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build());
            }
            return Mono.error(new IllegalStateException("Unexpected URL: " + method + " " + url));
        });

        MvcResult result = mockMvc.perform(post("/api/v1/gateway/register-flow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(request().asyncStarted())
                .andReturn();

        try {
            mockMvc.perform(asyncDispatch(result));
        } catch (Exception expected) {
            // Controller propagates RuntimeException after rollback; MockMvc surfaces it instead of mapping to 5xx.
        }

        assertThat(customerCreates.get()).isEqualTo(1);
        assertThat(authAttempts.get()).isEqualTo(1);
        assertThat(customerDeletes.get()).isEqualTo(1);
    }

    @Test
    void shouldRejectInvalidBody() throws Exception {
        wireExchange(req -> Mono.error(new IllegalStateException("Should not call WebClient on validation error")));

        String invalidBody = """
                {
                  "name": "",
                  "email": "not-an-email",
                  "password": "x",
                  "cpf": "abc",
                  "phone": "111"
                }
                """;

        mockMvc.perform(post("/api/v1/gateway/register-flow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }
}
