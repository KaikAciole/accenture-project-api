package br.com.accenture.api_gateway.controller;

import br.com.accenture.api_gateway.dto.GatewayRegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/gateway")
public class RegistrationOrchestratorController {

    private final WebClient.Builder webClientBuilder;

    public RegistrationOrchestratorController(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Value("${api.security.internal.secret:senha-secreta-microsservicos-123}")
    private String internalSecret;

    @PostMapping("/register-flow")
    public Mono<ResponseEntity<Void>> orchestrateRegistration(@RequestBody GatewayRegisterRequest request) {

        log.info("Iniciando fluxo de registro minimo para: {}", request.email());
        var customerMinimalRequest = Map.of("email", request.email());

        return webClientBuilder.build().post()
                .uri("http://localhost:8082/internal/customers")
                .header("X-Internal-Secret", internalSecret)
                .bodyValue(customerMinimalRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(10))
                .flatMap(customerResponse -> {

                    String customerIdStr = (String) customerResponse.get("id");
                    UUID customerId = UUID.fromString(customerIdStr);
                    log.info("Perfil criado. ID: {}. Registrando no Auth...", customerId);

                    var authRequest = Map.of(
                            "customerId", customerId,
                            "email", request.email(),
                            "password", request.password(),
                            "roles", Set.of("CUSTOMER")
                    );

                    return webClientBuilder.build().post()
                            .uri("http://localhost:8081/api/v1/auth/register")
                            .header("X-Internal-Secret", internalSecret)
                            .bodyValue(authRequest)
                            .retrieve()
                            .toBodilessEntity()
                            .timeout(Duration.ofSeconds(10))
                            .doOnSuccess(res -> log.info("Credenciais criadas com sucesso no Auth!"))
                            .onErrorResume(throwable -> {
                                log.error("Erro no Auth ou Timeout. Executando Rollback no Customer ID: {}", customerId);
                                return webClientBuilder.build().delete()
                                        .uri("http://localhost:8082/customers/" + customerId)
                                        .header("X-Internal-Secret", internalSecret)
                                        .retrieve()
                                        .toBodilessEntity()
                                        .timeout(Duration.ofSeconds(10))
                                        .then(Mono.error(new RuntimeException("Registration failed: " + throwable.getMessage())));
                            });
                })
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).build());
    }
}