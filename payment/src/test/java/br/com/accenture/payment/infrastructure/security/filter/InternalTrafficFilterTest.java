package br.com.accenture.payment.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicInteger;

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
    void doFilterPassesThroughWhenSecretMatches() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wallets/123");
        request.addHeader("X-Internal-Secret", SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainInvocations = new AtomicInteger();
        FilterChain chain = (req, res) -> chainInvocations.incrementAndGet();

        filter.doFilter(request, response, chain);

        assertThat(chainInvocations).hasValue(1);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterReturnsForbiddenWhenSecretIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wallets/123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainInvocations = new AtomicInteger();
        FilterChain chain = (req, res) -> chainInvocations.incrementAndGet();

        filter.doFilter(request, response, chain);

        assertThat(chainInvocations).hasValue(0);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("Acesso negado");
    }

    @Test
    void doFilterReturnsForbiddenWhenSecretIsWrong() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wallets/123");
        request.addHeader("X-Internal-Secret", "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainInvocations = new AtomicInteger();
        FilterChain chain = (req, res) -> chainInvocations.incrementAndGet();

        filter.doFilter(request, response, chain);

        assertThat(chainInvocations).hasValue(0);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void doFilterSkipsValidationForWebhookPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/webhooks/abacatepay");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainInvocations = new AtomicInteger();
        FilterChain chain = (req, res) -> chainInvocations.incrementAndGet();

        filter.doFilter(request, response, chain);

        assertThat(chainInvocations).hasValue(1);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
