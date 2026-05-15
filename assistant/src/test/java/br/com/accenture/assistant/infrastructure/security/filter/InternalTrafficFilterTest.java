package br.com.accenture.assistant.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class InternalTrafficFilterTest {

    private static final String SECRET = "test-internal-secret";

    private InternalTrafficFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new InternalTrafficFilter();
        ReflectionTestUtils.setField(filter, "internalSecret", SECRET);
        chain = mock(FilterChain.class);
    }

    @Test
    void shouldAllowRequestWithValidSecret() throws Exception {
        MockHttpServletRequest req = assistantRequest();
        req.addHeader("X-Internal-Secret", SECRET);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        assertThat(resp.getStatus()).isEqualTo(200);
        verify(chain, times(1)).doFilter(any(), any());
    }

    @Test
    void shouldBlockRequestWithInvalidSecret() throws Exception {
        MockHttpServletRequest req = assistantRequest();
        req.addHeader("X-Internal-Secret", "wrong-secret");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        assertThat(resp.getStatus()).isEqualTo(403);
        assertThat(resp.getContentAsString()).contains("Acesso negado");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldBlockRequestWithMissingSecret() throws Exception {
        MockHttpServletRequest req = assistantRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        assertThat(resp.getStatus()).isEqualTo(403);
        assertThat(resp.getContentAsString()).contains("Acesso negado");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldSkipFilterForSwaggerUi() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        req.setRequestURI("/swagger-ui/index.html");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        assertThat(resp.getStatus()).isEqualTo(200);
        verify(chain, times(1)).doFilter(any(), any());
    }

    @Test
    void shouldSkipFilterForApiDocs() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/v3/api-docs");
        req.setRequestURI("/v3/api-docs");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        assertThat(resp.getStatus()).isEqualTo(200);
        verify(chain, times(1)).doFilter(any(), any());
    }

    private MockHttpServletRequest assistantRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/assistant/ask");
        req.setRequestURI("/assistant/ask");
        return req;
    }
}
