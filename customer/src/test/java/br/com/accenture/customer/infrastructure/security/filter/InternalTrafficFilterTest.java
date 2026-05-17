package br.com.accenture.customer.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class InternalTrafficFilterTest {

    private static final String SECRET = "test-secret";

    private InternalTrafficFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalTrafficFilter();
        ReflectionTestUtils.setField(filter, "internalSecret", SECRET);
    }

    @Test
    void shouldNotFilter_returnsTrueForCepLookupPath() throws ServletException {
        Boolean result = ReflectionTestUtils.invokeMethod(
                filter, "shouldNotFilter", requestFor("/cep/lookup/01001000")
        );
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotFilter_returnsTrueForSwaggerUi() throws ServletException {
        Boolean result = ReflectionTestUtils.invokeMethod(
                filter, "shouldNotFilter", requestFor("/swagger-ui/index.html")
        );
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotFilter_returnsTrueForApiDocs() throws ServletException {
        Boolean result = ReflectionTestUtils.invokeMethod(
                filter, "shouldNotFilter", requestFor("/v3/api-docs/customer")
        );
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotFilter_returnsTrueForErrorPath() throws ServletException {
        Boolean result = ReflectionTestUtils.invokeMethod(
                filter, "shouldNotFilter", requestFor("/error")
        );
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotFilter_returnsFalseForBusinessPath() throws ServletException {
        Boolean result = ReflectionTestUtils.invokeMethod(
                filter, "shouldNotFilter", requestFor("/customers/123")
        );
        assertThat(result).isFalse();
    }

    @Test
    void doFilterInternal_shouldDelegateWhenSecretMatches() throws ServletException, IOException {
        MockHttpServletRequest request = requestFor("/customers/123");
        request.addHeader("X-Internal-Secret", SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void doFilterInternal_shouldBlockWhenSecretIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = requestFor("/customers/123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("Acesso negado");
        assertThat(((MockFilterChain) chain).getRequest()).isNull();
    }

    @Test
    void doFilterInternal_shouldBlockWhenSecretIsWrong() throws ServletException, IOException {
        MockHttpServletRequest request = requestFor("/customers/123");
        request.addHeader("X-Internal-Secret", "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Acesso negado");
        assertThat(chain.getRequest()).isNull();
    }

    private MockHttpServletRequest requestFor(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
