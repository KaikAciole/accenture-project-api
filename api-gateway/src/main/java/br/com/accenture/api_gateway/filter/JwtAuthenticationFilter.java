package br.com.accenture.api_gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${api.security.token.secret:my-super-secret-key-change-me-in-production}")
    private String jwtSecret;

    private final List<String> publicRoutes = List.of(
            "/api/v1/auth/**",
            "/api/v1/gateway/register-flow"
    );

    private final List<String> publicGetRoutes = List.of(
            "/products",
            "/products/**"
    );

    private final List<RouteRule> adminRoutes = List.of(
            new RouteRule("POST", "/products"),
            new RouteRule("PUT", "/products/**"),
            new RouteRule("DELETE", "/products/**"),
            new RouteRule("GET", "/customers"),
            new RouteRule("DELETE", "/customers/**"),
            new RouteRule("GET", "/orders"),
            new RouteRule("GET", "/wallets/owners/*/*/transactions")
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private record RouteRule(String method, String pattern) {}

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isPublic = publicRoutes.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
        if (isPublic) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            boolean isPublicGet = publicGetRoutes.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
            if (isPublicGet) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Acesso bloqueado: Rota {} sem token.", path);
            bloquearAcesso(response, "Acesso negado. Token ausente ou no formato incorreto.");
            return;
        }

        String token = authHeader.substring(7);

        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("auth-microservice")
                    .build();

            DecodedJWT decodedJWT = verifier.verify(token);
            String customerId = decodedJWT.getSubject();
            List<String> roles = extractRoles(decodedJWT);

            log.info("Token validado com sucesso para o usuario: {}", customerId);

            boolean isAdminRoute = adminRoutes.stream().anyMatch(rule ->
                    rule.method().equalsIgnoreCase(request.getMethod())
                            && pathMatcher.match(rule.pattern(), path)
            );
            if (isAdminRoute && !roles.contains("ADMIN")) {
                log.warn("Acesso bloqueado: rota {} {} requer role ADMIN, usuario {} possui {}", request.getMethod(), path, customerId, roles);
                bloquearAcessoForbidden(response, "Acesso negado. Permissao de admin necessaria.");
                return;
            }

            String rolesHeader = String.join(",", roles);
            HttpServletRequest wrappedRequest = new jakarta.servlet.http.HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("X-Customer-Id".equalsIgnoreCase(name)) {
                        return customerId;
                    }
                    if ("X-User-Roles".equalsIgnoreCase(name)) {
                        return rolesHeader;
                    }
                    return super.getHeader(name);
                }
            };

            filterChain.doFilter(wrappedRequest, response);

        } catch (Exception e) {
            log.error("Token expirado, corrompido ou adulterado: {}", e.getMessage());
            bloquearAcesso(response, "Token invalido ou expirado.");
        }
    }

    private void bloquearAcesso(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + mensagem + "\"}");
    }

    private void bloquearAcessoForbidden(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + mensagem + "\"}");
    }

    private static List<String> extractRoles(DecodedJWT decodedJWT) {
        List<String> roles = decodedJWT.getClaim("roles").asList(String.class);
        if (roles == null) {
            return Collections.emptyList();
        }
        return roles.stream().filter(Objects::nonNull).toList();
    }
}