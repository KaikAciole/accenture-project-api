package br.com.accenture.api_gateway.service;

import br.com.accenture.api_gateway.dto.SeedDataResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class SeedDataServiceTest {

    private static final String AUTH = "http://auth";
    private static final String CUSTOMER = "http://customer";
    private static final String INVENTORY = "http://inventory";
    private static final String ORDER = "http://order";
    private static final String PAYMENT = "http://payment";

    private SeedDataService newService(ExchangeFunction exchange) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        return new SeedDataService(builder, "secret", AUTH, CUSTOMER, INVENTORY, ORDER, PAYMENT);
    }

    private static ClientResponse json(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header("Content-Type", "application/json")
                .body(body)
                .build();
    }

    private static ClientResponse empty(HttpStatus status) {
        return ClientResponse.create(status).build();
    }

    private static String idJson() {
        return "{\"id\":\"" + UUID.randomUUID() + "\"}";
    }

    private ExchangeFunction happyDispatcher(Function<ClientRequest, Mono<ClientResponse>> overrides) {
        return req -> {
            Mono<ClientResponse> override = overrides.apply(req);
            if (override != null) {
                return override;
            }
            return defaultHappy(req);
        };
    }

    private Mono<ClientResponse> defaultHappy(ClientRequest req) {
        String method = req.method().name();
        String url = req.url().toString();

        if (method.equals("POST") && url.endsWith("/internal/customers")) {
            return Mono.just(json(HttpStatus.CREATED, idJson()));
        }
        if (method.equals("POST") && url.endsWith("/internal/auth/credentials/admin-register")) {
            return Mono.just(empty(HttpStatus.CREATED));
        }
        if (method.equals("POST") && url.endsWith("/api/v1/auth/register")) {
            return Mono.just(empty(HttpStatus.CREATED));
        }
        if (method.equals("POST") && url.matches(".*/customers/[^/]+/addresses")) {
            return Mono.just(json(HttpStatus.CREATED, idJson()));
        }
        if (method.equals("POST") && url.endsWith("/api/v1/auth/login")) {
            return Mono.just(json(HttpStatus.OK, "{\"accessToken\":\"jwt-token\"}"));
        }
        if (method.equals("POST") && url.endsWith("/products")) {
            return Mono.just(empty(HttpStatus.CREATED));
        }
        if (method.equals("GET") && url.matches(".*/wallets/owners/CUSTOMER/.+")) {
            return Mono.just(json(HttpStatus.OK, idJson()));
        }
        if (method.equals("PATCH") && url.contains("/wallets/") && url.endsWith("/credit")) {
            return Mono.just(empty(HttpStatus.OK));
        }
        if (method.equals("POST") && url.endsWith("/orders")) {
            return Mono.just(json(HttpStatus.CREATED, "{\"orderId\":\"order-xyz\"}"));
        }
        if (method.equals("POST") && url.endsWith("/payments")) {
            return Mono.just(json(HttpStatus.CREATED, "{\"id\":\"payment-xyz\"}"));
        }
        if (method.equals("PATCH") && url.matches(".*/payments/[^/]+/process")) {
            return Mono.just(empty(HttpStatus.OK));
        }
        if (method.equals("GET") && url.matches(".*/orders/[^/]+")) {
            return Mono.just(json(HttpStatus.OK, "{\"status\":\"PAID\"}"));
        }
        return Mono.error(new IllegalStateException("Unexpected request: " + method + " " + url));
    }

    @Test
    void happyPath_shouldCreateAdminCustomersProductsWalletsAndOrder() {
        SeedDataService service = newService(happyDispatcher(req -> null));

        SeedDataResponse response = service.run();

        assertThat(response.adminEmail()).isEqualTo("admin@accestore.com");
        assertThat(response.adminCreated()).isTrue();
        assertThat(response.customersCreated()).isEqualTo(3);
        assertThat(response.customersSkipped()).isZero();
        assertThat(response.productsCreated()).isEqualTo(8);
        assertThat(response.productsSkipped()).isZero();
        assertThat(response.walletsCredited()).isEqualTo(3);
        assertThat(response.seedOrderId()).isEqualTo("order-xyz");
        assertThat(response.seedOrderStatus()).isEqualTo("PAID");
        assertThat(response.warnings()).isEmpty();
    }

    @Test
    void adminConflict_shouldFindExistingAndStillRegisterFlow() {
        UUID existingAdminId = UUID.randomUUID();
        AtomicInteger adminPostCount = new AtomicInteger();

        SeedDataService service = newService(happyDispatcher(req -> {
            String method = req.method().name();
            String url = req.url().toString();
            if (method.equals("POST") && url.endsWith("/internal/customers")) {
                int n = adminPostCount.incrementAndGet();
                if (n == 1) {
                    return Mono.just(empty(HttpStatus.CONFLICT));
                }
            }
            if (method.equals("GET") && url.contains("/customers?size=200")) {
                return Mono.just(json(HttpStatus.OK, """
                        {"content":[{"id":"%s","email":"admin@accestore.com"}]}
                        """.formatted(existingAdminId)));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.adminCreated()).isFalse();
        assertThat(response.customersCreated()).isEqualTo(3);
    }

    @Test
    void adminAuthCredentialsConflict_shouldNotCountAsCreated() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("POST")
                    && url.endsWith("/internal/auth/credentials/admin-register")) {
                return Mono.just(empty(HttpStatus.CONFLICT));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.adminCreated()).isFalse();
        assertThat(response.warnings()).noneMatch(w -> w.contains("Falha ao criar admin no auth"));
    }

    @Test
    void adminAuthCredentialsError_shouldRecordWarning() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("POST")
                    && url.endsWith("/internal/auth/credentials/admin-register")) {
                return Mono.just(empty(HttpStatus.INTERNAL_SERVER_ERROR));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.adminCreated()).isFalse();
        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao criar admin no auth"));
    }

    @Test
    void adminConflictAndFindByEmailFails_shouldStillProceed() {
        AtomicInteger adminPostCount = new AtomicInteger();
        SeedDataService service = newService(happyDispatcher(req -> {
            String method = req.method().name();
            String url = req.url().toString();
            if (method.equals("POST") && url.endsWith("/internal/customers")) {
                int n = adminPostCount.incrementAndGet();
                if (n == 1) {
                    return Mono.just(empty(HttpStatus.CONFLICT));
                }
            }
            if (method.equals("GET") && url.contains("/customers?size=200")) {
                return Mono.just(empty(HttpStatus.INTERNAL_SERVER_ERROR));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.adminCreated()).isFalse();
        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao buscar customer por email"));
    }

    @Test
    void adminConflictAndPageHasNoMatch_shouldReturnNullExisting() {
        AtomicInteger adminPostCount = new AtomicInteger();
        SeedDataService service = newService(happyDispatcher(req -> {
            String method = req.method().name();
            String url = req.url().toString();
            if (method.equals("POST") && url.endsWith("/internal/customers")) {
                int n = adminPostCount.incrementAndGet();
                if (n == 1) {
                    return Mono.just(empty(HttpStatus.CONFLICT));
                }
            }
            if (method.equals("GET") && url.contains("/customers?size=200")) {
                return Mono.just(json(HttpStatus.OK, "{\"content\":[]}"));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.adminCreated()).isFalse();
    }

    @Test
    void adminConflictAndPageContentIsNull_shouldReturnNullExisting() {
        AtomicInteger adminPostCount = new AtomicInteger();
        SeedDataService service = newService(happyDispatcher(req -> {
            String method = req.method().name();
            String url = req.url().toString();
            if (method.equals("POST") && url.endsWith("/internal/customers")) {
                int n = adminPostCount.incrementAndGet();
                if (n == 1) {
                    return Mono.just(empty(HttpStatus.CONFLICT));
                }
            }
            if (method.equals("GET") && url.contains("/customers?size=200")) {
                return Mono.just(json(HttpStatus.OK, "{}"));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.adminCreated()).isFalse();
    }

    @Test
    void customerCreationGenericError_shouldRecordWarning() {
        AtomicInteger postCount = new AtomicInteger();
        SeedDataService service = newService(happyDispatcher(req -> {
            String method = req.method().name();
            String url = req.url().toString();
            if (method.equals("POST") && url.endsWith("/internal/customers")) {
                int n = postCount.incrementAndGet();
                if (n == 2) {
                    return Mono.just(empty(HttpStatus.INTERNAL_SERVER_ERROR));
                }
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.customersCreated()).isEqualTo(2);
        assertThat(response.customersSkipped()).isEqualTo(1);
        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao criar customer"));
    }

    @Test
    void customerConflictWithExistingAndAddresses_shouldFindExisting() {
        UUID existing = UUID.randomUUID();
        UUID existingAddr = UUID.randomUUID();
        AtomicInteger postCount = new AtomicInteger();

        SeedDataService service = newService(happyDispatcher(req -> {
            String method = req.method().name();
            String url = req.url().toString();
            if (method.equals("POST") && url.endsWith("/internal/customers")) {
                int n = postCount.incrementAndGet();
                if (n == 2) {
                    return Mono.just(empty(HttpStatus.CONFLICT));
                }
            }
            if (method.equals("GET") && url.contains("/customers?size=200")) {
                return Mono.just(json(HttpStatus.OK, """
                        {"content":[{"id":"%s","email":"maria@accestore.com"}]}
                        """.formatted(existing)));
            }
            if (method.equals("GET") && url.contains("/customers/" + existing + "/addresses")) {
                return Mono.just(json(HttpStatus.OK, """
                        {"content":[{"id":"%s"}]}
                        """.formatted(existingAddr)));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.customersCreated()).isEqualTo(2);
        assertThat(response.customersSkipped()).isEqualTo(1);
    }

    @Test
    void customerConflictExistingButNoAddresses_shouldCreateAddress() {
        UUID existing = UUID.randomUUID();
        AtomicInteger postCount = new AtomicInteger();

        SeedDataService service = newService(happyDispatcher(req -> {
            String method = req.method().name();
            String url = req.url().toString();
            if (method.equals("POST") && url.endsWith("/internal/customers")) {
                int n = postCount.incrementAndGet();
                if (n == 2) {
                    return Mono.just(empty(HttpStatus.CONFLICT));
                }
            }
            if (method.equals("GET") && url.contains("/customers?size=200")) {
                return Mono.just(json(HttpStatus.OK, """
                        {"content":[{"id":"%s","email":"maria@accestore.com"}]}
                        """.formatted(existing)));
            }
            if (method.equals("GET") && url.contains("/customers/" + existing + "/addresses")) {
                return Mono.just(json(HttpStatus.OK, "{\"content\":[]}"));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.customersSkipped()).isEqualTo(1);
    }

    @Test
    void customerConflictWithoutExisting_shouldRecordSkipWithoutAddress() {
        AtomicInteger postCount = new AtomicInteger();
        SeedDataService service = newService(happyDispatcher(req -> {
            String method = req.method().name();
            String url = req.url().toString();
            if (method.equals("POST") && url.endsWith("/internal/customers")) {
                int n = postCount.incrementAndGet();
                if (n == 2) {
                    return Mono.just(empty(HttpStatus.CONFLICT));
                }
            }
            if (method.equals("GET") && url.contains("/customers?size=200")) {
                return Mono.just(json(HttpStatus.OK, "{\"content\":[]}"));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.customersSkipped()).isEqualTo(1);
    }

    @Test
    void findOrCreateAddressErrorDuringSearch_shouldFallbackToCreate() {
        UUID existing = UUID.randomUUID();
        AtomicInteger postCount = new AtomicInteger();

        SeedDataService service = newService(happyDispatcher(req -> {
            String method = req.method().name();
            String url = req.url().toString();
            if (method.equals("POST") && url.endsWith("/internal/customers")) {
                int n = postCount.incrementAndGet();
                if (n == 2) {
                    return Mono.just(empty(HttpStatus.CONFLICT));
                }
            }
            if (method.equals("GET") && url.contains("/customers?size=200")) {
                return Mono.just(json(HttpStatus.OK, """
                        {"content":[{"id":"%s","email":"maria@accestore.com"}]}
                        """.formatted(existing)));
            }
            if (method.equals("GET") && url.contains("/customers/" + existing + "/addresses")) {
                return Mono.just(empty(HttpStatus.INTERNAL_SERVER_ERROR));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao buscar enderecos"));
    }

    @Test
    void addressCreationFails_shouldRecordWarning() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String method = req.method().name();
            String url = req.url().toString();
            if (method.equals("POST") && url.matches(".*/customers/[^/]+/addresses")) {
                return Mono.just(empty(HttpStatus.INTERNAL_SERVER_ERROR));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao criar endereco"));
    }

    @Test
    void authRegisterConflictForCustomer_shouldStaySilent() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("POST") && url.endsWith("/api/v1/auth/register")) {
                return Mono.just(empty(HttpStatus.CONFLICT));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.warnings()).noneMatch(w -> w.contains("Falha ao registrar credenciais"));
    }

    @Test
    void authRegisterErrorForCustomer_shouldRecordWarning() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("POST") && url.endsWith("/api/v1/auth/register")) {
                return Mono.just(empty(HttpStatus.INTERNAL_SERVER_ERROR));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao registrar credenciais"));
    }

    @Test
    void adminLoginFails_shouldSkipProductsAndRecordWarning() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("POST") && url.endsWith("/api/v1/auth/login")) {
                return Mono.just(empty(HttpStatus.UNAUTHORIZED));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.productsCreated()).isZero();
        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao logar como admin"));
    }

    @Test
    void loginEmptyBody_shouldBeTreatedAsError() {
        AtomicInteger loginCalls = new AtomicInteger();
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("POST") && url.endsWith("/api/v1/auth/login")) {
                int n = loginCalls.incrementAndGet();
                if (n == 1) {
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .build());
                }
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao logar como admin"));
    }

    @Test
    void productConflict_shouldSkip() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("POST") && url.endsWith("/products")) {
                return Mono.just(empty(HttpStatus.CONFLICT));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.productsCreated()).isZero();
        assertThat(response.productsSkipped()).isEqualTo(8);
    }

    @Test
    void productGenericError_shouldRecordWarning() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("POST") && url.endsWith("/products")) {
                return Mono.just(empty(HttpStatus.INTERNAL_SERVER_ERROR));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao criar produto"));
    }

    @Test
    void walletLookupReturnsEmpty_shouldNotCredit() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("GET") && url.contains("/wallets/owners/CUSTOMER/")) {
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .build());
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.walletsCredited()).isZero();
    }

    @Test
    void walletCreditError_shouldRecordWarning() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("PATCH") && url.endsWith("/credit")) {
                return Mono.just(empty(HttpStatus.INTERNAL_SERVER_ERROR));
            }
            return null;
        }));

        SeedDataService localService = service;
        SeedDataResponse response = localService.run();

        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao creditar wallet"));
    }

    @Test
    void buyerLoginFails_shouldRecordWarningAboutSeedOrder() {
        AtomicInteger loginCalls = new AtomicInteger();
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("POST") && url.endsWith("/api/v1/auth/login")) {
                int n = loginCalls.incrementAndGet();
                if (n == 2) {
                    return Mono.error(new RuntimeException("buyer login boom"));
                }
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.seedOrderId()).isNull();
        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao criar pedido seed"));
    }

    @Test
    void orderCreationReturnsError_shouldNotProduceSeedOrder() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("POST") && url.endsWith("/orders")) {
                return Mono.just(empty(HttpStatus.INTERNAL_SERVER_ERROR));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.seedOrderId()).isNull();
        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao criar pedido seed"));
    }

    @Test
    void paymentCreationFails_shouldReportPending() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("POST") && url.endsWith("/payments")) {
                return Mono.just(empty(HttpStatus.INTERNAL_SERVER_ERROR));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.seedOrderId()).isEqualTo("order-xyz");
        assertThat(response.seedOrderStatus()).isEqualTo("PENDING (payment not created)");
        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao criar payment seed"));
    }

    @Test
    void paymentProcessingFails_shouldRecordWarning() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("PATCH") && url.matches(".*/payments/[^/]+/process")) {
                return Mono.just(empty(HttpStatus.INTERNAL_SERVER_ERROR));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao processar payment seed"));
    }

    @Test
    void fetchOrderStatusFails_shouldRecordWarning() {
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("GET") && url.matches(".*/orders/[^/]+")) {
                return Mono.just(empty(HttpStatus.INTERNAL_SERVER_ERROR));
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.warnings()).anyMatch(w -> w.contains("Falha ao buscar status do pedido seed"));
    }

    @Test
    void postInternalCustomerReturnsEmptyBody_shouldTreatAsNull() {
        AtomicInteger postCount = new AtomicInteger();
        SeedDataService service = newService(happyDispatcher(req -> {
            String url = req.url().toString();
            if (req.method().name().equals("POST") && url.endsWith("/internal/customers")) {
                int n = postCount.incrementAndGet();
                if (n == 1) {
                    return Mono.just(ClientResponse.create(HttpStatus.CREATED)
                            .header("Content-Type", "application/json")
                            .build());
                }
            }
            return null;
        }));

        SeedDataResponse response = service.run();

        assertThat(response.adminCreated()).isFalse();
    }
}
