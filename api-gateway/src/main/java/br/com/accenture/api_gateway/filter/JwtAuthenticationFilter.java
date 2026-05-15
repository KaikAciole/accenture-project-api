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
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${api.security.token.secret:my-super-secret-key-change-me-in-production}")
    private String jwtSecret;

    private final List<String> publicRoutes = List.of(
            "/api/v1/auth/**",
            "/api/v1/gateway/register-flow"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        boolean isPublic = publicRoutes.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
        if (isPublic) {
            filterChain.doFilter(request, response);
            return;
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

            log.info("Token validado com sucesso para o usuario: {}", customerId);

            HttpServletRequest wrappedRequest = new jakarta.servlet.http.HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("X-Customer-Id".equalsIgnoreCase(name)) {
                        return customerId;
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
}