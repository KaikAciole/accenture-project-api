package br.com.accenture.api_gateway.controller;

import br.com.accenture.api_gateway.dto.GatewayRegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

    @PostMapping("/register-flow")
    public Mono<ResponseEntity<Void>> orchestrateRegistration(@RequestBody GatewayRegisterRequest request) {

        log.info("Iniciando fluxo de registro minimo para: {}", request.email());
        var customerMinimalRequest = Map.of("email", request.email());

        return webClientBuilder.build().post()
                .uri("http://localhost:8082/internal/customers")
                .bodyValue(customerMinimalRequest)
                .retrieve()
                .bodyToMono(Map.class)
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
                            .bodyValue(authRequest)
                            .retrieve()
                            .toBodilessEntity()
                            .doOnSuccess(res -> log.info("Credenciais criadas com sucesso no Auth!"))
                            .onErrorResume(throwable -> {
                                log.error("Erro no Auth. Executando Rollback no Customer ID: {}", customerId);
                                return webClientBuilder.build().delete()
                                        .uri("http://localhost:8082/customers/" + customerId)
                                        .retrieve()
                                        .toBodilessEntity()
                                        .then(Mono.error(new RuntimeException("Registration failed")));
                            });
                })
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).build());
    }
}