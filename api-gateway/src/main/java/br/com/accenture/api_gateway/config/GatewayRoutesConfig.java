package br.com.accenture.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.addRequestHeader;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.rewritePath;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    @Value("${api.security.internal.secret:senha-secreta-microsservicos-123}")
    private String internalSecret;

    @Bean
    public RouterFunction<ServerResponse> gatewayRouter() {

        RouterFunction<ServerResponse> authRoute = route("auth-service")
                .route(path("/api/v1/auth/**"), http())
                .before(addRequestHeader("X-Internal-Secret", internalSecret))
                .before(uri("http://localhost:8081"))
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

        return authRoute
                .and(customerRoute)
                .and(inventoryRoute)
                .and(notificationRoute)
                .and(orderRoute)
                .and(paymentRoute)
                .and(assistantRoute);
    }
}