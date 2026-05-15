package br.com.accenture.order.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class InternalTrafficFilter extends OncePerRequestFilter {

    private final String internalSecret;

    public InternalTrafficFilter(String internalSecret) {
        this.internalSecret = internalSecret;
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