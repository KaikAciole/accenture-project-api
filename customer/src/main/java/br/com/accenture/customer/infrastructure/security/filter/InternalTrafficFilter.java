package br.com.accenture.customer.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalTrafficFilter extends OncePerRequestFilter {

    @Value("${api.security.internal.secret:senha-secreta-microsservicos-12345}")
    private String internalSecret;

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