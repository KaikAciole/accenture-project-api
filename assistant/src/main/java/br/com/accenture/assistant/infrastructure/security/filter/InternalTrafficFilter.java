package br.com.accenture.assistant.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Order(1)
public class InternalTrafficFilter extends OncePerRequestFilter {

    @Value("${api.security.internal.secret:senha-secreta-microsservicos-1234}")
    private String internalSecret;

    private final List<String> excludedPaths = List.of(
            "/cep/lookup/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/error"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return excludedPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String secretHeader = request.getHeader("X-Internal-Secret");

        if (secretHeader == null || !secretHeader.equals(internalSecret)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Acesso negado. Requisicao deve passar obrigatoriamente pelo API Gateway.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}