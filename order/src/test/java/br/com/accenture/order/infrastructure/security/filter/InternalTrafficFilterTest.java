package br.com.accenture.order.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InternalTrafficFilterTest {

    private static final String SECRET = "segredo-interno";

    private InternalTrafficFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalTrafficFilter(SECRET);
    }

    @Test
    @DisplayName("Deve permitir requisicao quando o header X-Internal-Secret esta correto")
    void shouldPassThroughWhenSecretHeaderIsValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Secret", SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve retornar 403 quando o header X-Internal-Secret esta ausente")
    void shouldReturn403WhenSecretHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("Acesso negado");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Deve retornar 403 quando o header X-Internal-Secret esta incorreto")
    void shouldReturn403WhenSecretHeaderIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Secret", "valor-errado");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Acesso negado");
        verify(chain, never()).doFilter(any(), any());
    }
}
