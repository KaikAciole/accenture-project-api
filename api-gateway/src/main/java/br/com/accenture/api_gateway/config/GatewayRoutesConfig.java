package br.com.accenture.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.addRequestHeader;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.rewritePath;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.setPath;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    @Value("${api.security.internal.secret:senha-secreta-microsservicos-1234}")
    private String internalSecret;

    @Bean
    public RouterFunction<ServerResponse> gatewayRouter() {

        RouterFunction<ServerResponse> authRoute = route("auth-service")
                .route(path("/api/v1/auth/**"), http())
                .before(addRequestHeader("X-Internal-Secret", internalSecret))
                .before(uri("http://localhost:8081"))
                .build();

        RouterFunction<ServerResponse> cepRoute = route("cep-lookup")
                .route(path("/api/v1/cep/lookup"), http())
                .before(setPath("/cep/lookup"))
                .before(addRequestHeader("X-Internal-Secret", internalSecret))
                .before(uri("http://localhost:8082"))
                .build();

        RouterFunction<ServerResponse> orderPublicRoute = route("order-customer-list")
                .route(path("/api/v1/orders/my-orders"), http())
                .before(addRequestHeader("X-Internal-Secret", internalSecret))
                .before(uri("http://localhost:8085"))
                .build();

        RouterFunction<ServerResponse> customerRoute = route("customer-service")
                .route(path("/customers/**"), http())
                .before(addRequestHeader("X-Internal-Secret", internalSecret))
                .before(uri("http://localhost:8082"))
                .build();

        RouterFunction<ServerResponse> inventoryRoute = route("inventory-service")
                .route(path("/products/**").or(path("/stock-reservations/**")), http())
                .before(addRequestHeader("X-Internal-Secret", internalSecret))
                .before(uri("http://localhost:8083"))
                .build();

        RouterFunction<ServerResponse> notificationRoute = route("notification-service")
                .route(path("/notifications/**"), http())
                .before(addRequestHeader("X-Internal-Secret", internalSecret))
                .before(uri("http://localhost:8084"))
                .build();

        RouterFunction<ServerResponse> orderRoute = route("order-service")
                .route(path("/orders/**"), http())
                .before(addRequestHeader("X-Internal-Secret", internalSecret))
                .before(uri("http://localhost:8085"))
                .build();

        RouterFunction<ServerResponse> paymentRoute = route("payment-service")
                .route(path("/payments/**").or(path("/wallets/**")), http())
                .before(addRequestHeader("X-Internal-Secret", internalSecret))
                .before(uri("http://localhost:8086"))
                .build();

        RouterFunction<ServerResponse> assistantRoute = route("assistant-service")
                .route(path("/api/v1/assistant/**"), http())
                .before(rewritePath("/api/v1/assistant/(?<segment>.*)", "/assistant/${segment}"))
                .before(addRequestHeader("X-Internal-Secret", internalSecret))
                .before(uri("http://localhost:8087"))
                .build();

        RouterFunction<ServerResponse> authDocsRoute = docsRoute("auth-docs", "/auth", "http://localhost:8081");
        RouterFunction<ServerResponse> customerDocsRoute = docsRoute("customer-docs", "/customer", "http://localhost:8082");
        RouterFunction<ServerResponse> inventoryDocsRoute = docsRoute("inventory-docs", "/inventory", "http://localhost:8083");
        RouterFunction<ServerResponse> notificationDocsRoute = docsRoute("notification-docs", "/notification", "http://localhost:8084");
        RouterFunction<ServerResponse> orderDocsRoute = docsRoute("order-docs", "/order", "http://localhost:8085");
        RouterFunction<ServerResponse> paymentDocsRoute = docsRoute("payment-docs", "/payment", "http://localhost:8086");
        RouterFunction<ServerResponse> assistantDocsRoute = docsRoute("assistant-docs", "/assistant", "http://localhost:8087");

        return authRoute
                .and(cepRoute)
                .and(orderPublicRoute)
                .and(customerRoute)
                .and(inventoryRoute)
                .and(notificationRoute)
                .and(orderRoute)
                .and(paymentRoute)
                .and(assistantRoute)
                .and(authDocsRoute)
                .and(customerDocsRoute)
                .and(inventoryDocsRoute)
                .and(notificationDocsRoute)
                .and(orderDocsRoute)
                .and(paymentDocsRoute)
                .and(assistantDocsRoute);
    }

    private RouterFunction<ServerResponse> docsRoute(String id, String prefix, String targetUri) {
        return route(id)
                .route(path(prefix + "/swagger-ui/**")
                        .or(path(prefix + "/swagger-ui.html"))
                        .or(path(prefix + "/v3/api-docs/**"))
                        .or(path(prefix + "/v3/api-docs"))
                        .or(path(prefix + "/h2-console/**"))
                        .or(path(prefix + "/h2-console")), http())
                .before(rewritePath(prefix + "/(?<segment>.*)", "/${segment}"))
                .before(addRequestHeader("X-Internal-Secret", internalSecret))
                .before(uri(targetUri))
                .build();
    }
}