package br.com.accenture.assistant.infrastructure.security.ratelimit;

import br.com.accenture.assistant.infrastructure.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties(
                new RateLimitProperties.Limits(2, 5),
                new RateLimitProperties.Limits(3, 10),
                new RateLimitProperties.Concurrency(20)
        );
        BucketRegistry registry = new BucketRegistry(properties);
        filter = new RateLimitFilter(registry);
        chain = mock(FilterChain.class);
    }

    @Test
    void shouldAllowRequestsWithinUserMinuteLimit() throws Exception {
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = assistantRequest();
            req.addHeader("X-Customer-Id", "user-1");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, chain);
            assertThat(resp.getStatus()).isEqualTo(200);
        }
        verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldReturn429WhenUserMinuteLimitExceeded() throws Exception {
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = assistantRequest();
            req.addHeader("X-Customer-Id", "user-2");
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest req = assistantRequest();
        req.addHeader("X-Customer-Id", "user-2");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);

        assertThat(resp.getStatus()).isEqualTo(429);
        assertThat(resp.getHeader("Retry-After")).isNotNull();
        assertThat(resp.getContentAsString()).contains("Rate limit exceeded");
        verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldFallbackToIpLimitsWhenUserHeaderMissing() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = assistantRequest();
            req.setRemoteAddr("10.0.0.1");
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest req = assistantRequest();
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);

        assertThat(resp.getStatus()).isEqualTo(429);
        verify(chain, times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldNotFilterNonAssistantPaths() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/v3/api-docs");
        req.setRequestURI("/v3/api-docs");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);
        assertThat(resp.getStatus()).isEqualTo(200);
        verify(chain, times(1)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldKeepBucketsSeparatePerUser() throws Exception {
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = assistantRequest();
            req.addHeader("X-Customer-Id", "user-A");
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest reqB = assistantRequest();
        reqB.addHeader("X-Customer-Id", "user-B");
        MockHttpServletResponse respB = new MockHttpServletResponse();
        filter.doFilter(reqB, respB, chain);

        assertThat(respB.getStatus()).isEqualTo(200);
        verify(chain, times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private MockHttpServletRequest assistantRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/assistant/ask");
        req.setRequestURI("/assistant/ask");
        return req;
    }
}
