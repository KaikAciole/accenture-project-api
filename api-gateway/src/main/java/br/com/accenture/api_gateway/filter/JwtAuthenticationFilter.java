package br.com.accenture.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${api.security.token.secret:sua-chave-secreta-super-segura-aqui-com-pelo-menos-32-caracteres}")
    private String jwtSecret;

    private final List<String> publicRoutes = List.of(
            "/api/v1/auth/**",
            "/api/v1/gateway/register-flow"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
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
            SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.info("Token validado com sucesso para o usuário: {}", claims.getSubject());

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Token expirado, corrompido ou adulterado.");
            bloquearAcesso(response, "Token invalido ou expirado.");
        }
    }

    private void bloquearAcesso(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + mensagem + "\"}");
    }
}