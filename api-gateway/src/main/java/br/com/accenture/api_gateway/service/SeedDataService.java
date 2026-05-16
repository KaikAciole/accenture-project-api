package br.com.accenture.api_gateway.service;

import br.com.accenture.api_gateway.dto.SeedDataResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class SeedDataService {

    private static final String ADMIN_EMAIL = "admin@accestore.com";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String DEFAULT_PASSWORD = "Senha@123";

    private final WebClient.Builder webClientBuilder;
    private final String internalSecret;
    private final String authBaseUrl;
    private final String customerBaseUrl;
    private final String inventoryBaseUrl;
    private final String orderBaseUrl;
    private final String paymentBaseUrl;

    public SeedDataService(
            WebClient.Builder webClientBuilder,
            @Value("${api.security.internal.secret:senha-secreta-microsservicos-1234}") String internalSecret,
            @Value("${seed.auth-url:http://localhost:8081}") String authBaseUrl,
            @Value("${seed.customer-url:http://localhost:8082}") String customerBaseUrl,
            @Value("${seed.inventory-url:http://localhost:8083}") String inventoryBaseUrl,
            @Value("${seed.order-url:http://localhost:8085}") String orderBaseUrl,
            @Value("${seed.payment-url:http://localhost:8086}") String paymentBaseUrl
    ) {
        this.webClientBuilder = webClientBuilder;
        this.internalSecret = internalSecret;
        this.authBaseUrl = authBaseUrl;
        this.customerBaseUrl = customerBaseUrl;
        this.inventoryBaseUrl = inventoryBaseUrl;
        this.orderBaseUrl = orderBaseUrl;
        this.paymentBaseUrl = paymentBaseUrl;
    }

    public SeedDataResponse run() {
        List<String> warnings = new ArrayList<>();

        AdminResult admin = createAdmin(warnings);

        List<CustomerSeed> seeds = customerSeeds();
        List<CustomerResult> customers = new ArrayList<>();
        int customersCreated = 0;
        int customersSkipped = 0;
        for (CustomerSeed seed : seeds) {
            CustomerResult result = createCustomerWithAddress(seed, warnings);
            customers.add(result);
            if (result.created) customersCreated++;
            else customersSkipped++;
        }

        String adminToken = null;
        try {
            adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        } catch (Exception ex) {
            warnings.add("Falha ao logar como admin: " + ex.getMessage());
        }

        int productsCreated = 0;
        int productsSkipped = 0;
        if (adminToken != null) {
            for (ProductSeed product : productSeeds()) {
                Boolean created = createProduct(product, adminToken, warnings);
                if (Boolean.TRUE.equals(created)) productsCreated++;
                else if (Boolean.FALSE.equals(created)) productsSkipped++;
            }
        }

        int walletsCredited = 0;
        for (CustomerResult c : customers) {
            if (c.customerId == null) continue;
            Boolean ok = creditWallet(c.customerId, new BigDecimal("1000.00"), warnings);
            if (Boolean.TRUE.equals(ok)) walletsCredited++;
        }

        String seedOrderId = null;
        String seedOrderStatus = null;
        CustomerResult buyer = customers.isEmpty() ? null : customers.get(0);
        if (buyer != null && buyer.customerId != null && buyer.addressId != null) {
            try {
                String customerToken = login(buyer.email, DEFAULT_PASSWORD);
                String orderId = createOrderForSeed(buyer, customerToken, warnings);
                if (orderId != null) {
                    seedOrderId = orderId;
                    sleepFor(Duration.ofSeconds(4));

                    String paymentId = createPayment(orderId, buyer.customerId, new BigDecimal("100.00"), warnings);
                    if (paymentId != null) {
                        processPayment(paymentId, warnings);
                        sleepFor(Duration.ofSeconds(2));
                        seedOrderStatus = fetchOrderStatus(orderId, customerToken, warnings);
                    } else {
                        seedOrderStatus = "PENDING (payment not created)";
                    }
                }
            } catch (Exception ex) {
                warnings.add("Falha ao criar pedido seed: " + ex.getMessage());
            }
        }

        return new SeedDataResponse(
                ADMIN_EMAIL,
                admin.created,
                customersCreated,
                customersSkipped,
                productsCreated,
                productsSkipped,
                walletsCredited,
                seedOrderId,
                seedOrderStatus,
                warnings
        );
    }

    private AdminResult createAdmin(List<String> warnings) {
        UUID customerId = postInternalCustomer(
                "Admin Loja", ADMIN_EMAIL, "00000000000", "11900000000",
                warnings, "admin-customer"
        );

        if (customerId == null) {
            UUID existing = findCustomerIdByEmail(ADMIN_EMAIL, warnings);
            return new AdminResult(false, existing);
        }

        try {
            webClientBuilder.build().post()
                    .uri(authBaseUrl + "/internal/auth/credentials/admin-register")
                    .header("X-Internal-Secret", internalSecret)
                    .bodyValue(Map.of(
                            "customerId", customerId,
                            "email", ADMIN_EMAIL,
                            "password", ADMIN_PASSWORD
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .block();
            return new AdminResult(true, customerId);
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                return new AdminResult(false, customerId);
            }
            warnings.add("Falha ao criar admin no auth: " + ex.getMessage());
            return new AdminResult(false, customerId);
        }
    }

    private List<CustomerSeed> customerSeeds() {
        return List.of(
                new CustomerSeed(
                        "Maria Silva", "maria@accestore.com", "11111111111", "11911111111",
                        "Rua Augusta", "100", "Apto 101", "Consolacao", "Sao Paulo", "SP", "01304000"
                ),
                new CustomerSeed(
                        "Joao Souza", "joao@accestore.com", "22222222222", "11922222222",
                        "Rua Bela Cintra", "250", null, "Consolacao", "Sao Paulo", "SP", "01415000"
                ),
                new CustomerSeed(
                        "Carlos Pereira", "carlos@accestore.com", "33333333333", "11933333333",
                        "Av Paulista", "1000", "Sala 50", "Bela Vista", "Sao Paulo", "SP", "01310100"
                )
        );
    }

    private CustomerResult createCustomerWithAddress(CustomerSeed seed, List<String> warnings) {
        UUID customerId = postInternalCustomer(
                seed.name, seed.email, seed.cpf, seed.phone,
                warnings, seed.email
        );

        if (customerId != null) {
            try {
                webClientBuilder.build().post()
                        .uri(authBaseUrl + "/api/v1/auth/register")
                        .header("X-Internal-Secret", internalSecret)
                        .bodyValue(Map.of(
                                "customerId", customerId,
                                "email", seed.email,
                                "password", DEFAULT_PASSWORD,
                                "roles", List.of("CUSTOMER")
                        ))
                        .retrieve()
                        .toBodilessEntity()
                        .timeout(Duration.ofSeconds(10))
                        .block();
            } catch (WebClientResponseException ex) {
                if (ex.getStatusCode().value() != 409) {
                    warnings.add("Falha ao registrar credenciais para " + seed.email + ": " + ex.getMessage());
                }
            }

            UUID addressId = createAddress(customerId, seed, warnings);
            return new CustomerResult(true, customerId, addressId, seed.email);
        }

        UUID existingId = findCustomerIdByEmail(seed.email, warnings);
        UUID existingAddressId = (existingId != null)
                ? findOrCreateAddress(existingId, seed, warnings)
                : null;
        return new CustomerResult(false, existingId, existingAddressId, seed.email);
    }

    private UUID postInternalCustomer(String name, String email, String cpf, String phone,
                                      List<String> warnings, String label) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = webClientBuilder.build().post()
                    .uri(customerBaseUrl + "/internal/customers")
                    .header("X-Internal-Secret", internalSecret)
                    .bodyValue(Map.of(
                            "name", name,
                            "email", email,
                            "cpf", cpf,
                            "phone", phone
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (body == null) return null;
            return UUID.fromString((String) body.get("id"));
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                return null;
            }
            warnings.add("Falha ao criar customer " + label + ": " + ex.getMessage());
            return null;
        }
    }

    private UUID findCustomerIdByEmail(String email, List<String> warnings) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> page = webClientBuilder.build().get()
                    .uri(customerBaseUrl + "/customers?size=200")
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (page == null) return null;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
            if (content == null) return null;
            return content.stream()
                    .filter(c -> email.equalsIgnoreCase((String) c.get("email")))
                    .map(c -> UUID.fromString((String) c.get("id")))
                    .findFirst()
                    .orElse(null);
        } catch (Exception ex) {
            warnings.add("Falha ao buscar customer por email " + email + ": " + ex.getMessage());
            return null;
        }
    }

    private UUID createAddress(UUID customerId, CustomerSeed seed, List<String> warnings) {
        try {
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("street", seed.street);
            bodyMap.put("number", seed.number);
            bodyMap.put("complement", seed.complement);
            bodyMap.put("neighborhood", seed.neighborhood);
            bodyMap.put("city", seed.city);
            bodyMap.put("state", seed.state);
            bodyMap.put("zipCode", seed.zipCode);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = webClientBuilder.build().post()
                    .uri(customerBaseUrl + "/customers/" + customerId + "/addresses")
                    .header("X-Internal-Secret", internalSecret)
                    .bodyValue(bodyMap)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (body == null) return null;
            return UUID.fromString((String) body.get("id"));
        } catch (Exception ex) {
            warnings.add("Falha ao criar endereco para " + seed.email + ": " + ex.getMessage());
            return null;
        }
    }

    private UUID findOrCreateAddress(UUID customerId, CustomerSeed seed, List<String> warnings) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> page = webClientBuilder.build().get()
                    .uri(customerBaseUrl + "/customers/" + customerId + "/addresses?size=10")
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (page != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
                if (content != null && !content.isEmpty()) {
                    return UUID.fromString((String) content.get(0).get("id"));
                }
            }
        } catch (Exception ex) {
            warnings.add("Falha ao buscar enderecos de " + customerId + ": " + ex.getMessage());
        }
        return createAddress(customerId, seed, warnings);
    }

    private String login(String email, String password) {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = webClientBuilder.build().post()
                .uri(authBaseUrl + "/api/v1/auth/login")
                .header("X-Internal-Secret", internalSecret)
                .bodyValue(Map.of("email", email, "password", password))
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(10))
                .block();
        if (body == null) {
            throw new IllegalStateException("Login retornou body vazio para " + email);
        }
        return (String) body.get("accessToken");
    }

    private List<ProductSeed> productSeeds() {
        return List.of(
                new ProductSeed("NB-001", "Notebook Acer Aspire", "Notebooks", new BigDecimal("3500.00"), 20, "https://placehold.co/300x300?text=Notebook"),
                new ProductSeed("MS-001", "Mouse Logitech M170", "Acessorios", new BigDecimal("80.00"), 100, "https://placehold.co/300x300?text=Mouse"),
                new ProductSeed("KB-001", "Teclado Mecanico Redragon", "Acessorios", new BigDecimal("250.00"), 50, "https://placehold.co/300x300?text=Teclado"),
                new ProductSeed("MN-001", "Monitor 24 LG IPS", "Monitores", new BigDecimal("1200.00"), 30, "https://placehold.co/300x300?text=Monitor"),
                new ProductSeed("HS-001", "Headset HyperX Cloud", "Audio", new BigDecimal("450.00"), 40, "https://placehold.co/300x300?text=Headset"),
                new ProductSeed("WC-001", "Webcam Logitech C920", "Acessorios", new BigDecimal("600.00"), 25, "https://placehold.co/300x300?text=Webcam"),
                new ProductSeed("CB-001", "Cabo HDMI 2m", "Cabos", new BigDecimal("20.00"), 200, "https://placehold.co/300x300?text=HDMI"),
                new ProductSeed("PD-001", "Pendrive Sandisk 64GB", "Armazenamento", new BigDecimal("70.00"), 80, "https://placehold.co/300x300?text=Pendrive")
        );
    }

    private Boolean createProduct(ProductSeed product, String adminToken, List<String> warnings) {
        try {
            webClientBuilder.build().post()
                    .uri(inventoryBaseUrl + "/products")
                    .header("X-Internal-Secret", internalSecret)
                    .header("Authorization", "Bearer " + adminToken)
                    .bodyValue(Map.of(
                            "sku", product.sku,
                            "name", product.name,
                            "category", product.category,
                            "basePrice", product.basePrice,
                            "stockQuantity", product.stockQuantity,
                            "imageUrl", product.imageUrl
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .block();
            return true;
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                return false;
            }
            warnings.add("Falha ao criar produto " + product.sku + ": " + ex.getMessage());
            return null;
        }
    }

    private Boolean creditWallet(UUID customerId, BigDecimal amount, List<String> warnings) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> wallet = webClientBuilder.build().get()
                    .uri(paymentBaseUrl + "/wallets/owners/CUSTOMER/" + customerId)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (wallet == null) return false;
            UUID walletId = UUID.fromString((String) wallet.get("id"));

            webClientBuilder.build().patch()
                    .uri(paymentBaseUrl + "/wallets/" + walletId + "/credit")
                    .header("X-Internal-Secret", internalSecret)
                    .bodyValue(Map.of(
                            "amount", amount,
                            "reason", "TOP_UP"
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .block();
            return true;
        } catch (Exception ex) {
            warnings.add("Falha ao creditar wallet de " + customerId + ": " + ex.getMessage());
            return false;
        }
    }

    private String createOrderForSeed(CustomerResult buyer, String token, List<String> warnings) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = webClientBuilder.build().post()
                    .uri(orderBaseUrl + "/orders")
                    .header("X-Internal-Secret", internalSecret)
                    .header("Authorization", "Bearer " + token)
                    .header("X-Customer-Id", buyer.customerId.toString())
                    .bodyValue(Map.of(
                            "addressId", buyer.addressId,
                            "items", List.of(
                                    Map.of("sku", "MS-001", "quantity", 1, "unitPrice", new BigDecimal("80.00")),
                                    Map.of("sku", "CB-001", "quantity", 1, "unitPrice", new BigDecimal("20.00"))
                            )
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (body == null) return null;
            return (String) body.get("orderId");
        } catch (Exception ex) {
            warnings.add("Falha ao criar pedido seed: " + ex.getMessage());
            return null;
        }
    }

    private String createPayment(String orderId, UUID customerId, BigDecimal amount, List<String> warnings) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = webClientBuilder.build().post()
                    .uri(paymentBaseUrl + "/payments")
                    .header("X-Internal-Secret", internalSecret)
                    .bodyValue(Map.of(
                            "orderId", orderId,
                            "customerId", customerId,
                            "amount", amount,
                            "method", "WALLET"
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (body == null) return null;
            return (String) body.get("id");
        } catch (Exception ex) {
            warnings.add("Falha ao criar payment seed: " + ex.getMessage());
            return null;
        }
    }

    private void processPayment(String paymentId, List<String> warnings) {
        try {
            webClientBuilder.build().patch()
                    .uri(paymentBaseUrl + "/payments/" + paymentId + "/process")
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .block();
        } catch (Exception ex) {
            warnings.add("Falha ao processar payment seed: " + ex.getMessage());
        }
    }

    private String fetchOrderStatus(String orderId, String token, List<String> warnings) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = webClientBuilder.build().get()
                    .uri(orderBaseUrl + "/orders/" + orderId)
                    .header("X-Internal-Secret", internalSecret)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (body == null) return null;
            return (String) body.get("status");
        } catch (Exception ex) {
            warnings.add("Falha ao buscar status do pedido seed: " + ex.getMessage());
            return null;
        }
    }

    private void sleepFor(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private record AdminResult(boolean created, UUID customerId) {}

    private record CustomerSeed(
            String name, String email, String cpf, String phone,
            String street, String number, String complement,
            String neighborhood, String city, String state, String zipCode
    ) {}

    private static class CustomerResult {
        final boolean created;
        final UUID customerId;
        final UUID addressId;
        final String email;

        CustomerResult(boolean created, UUID customerId, UUID addressId, String email) {
            this.created = created;
            this.customerId = customerId;
            this.addressId = addressId;
            this.email = email;
        }
    }

    private record ProductSeed(
            String sku, String name, String category,
            BigDecimal basePrice, Integer stockQuantity, String imageUrl
    ) {}
}
