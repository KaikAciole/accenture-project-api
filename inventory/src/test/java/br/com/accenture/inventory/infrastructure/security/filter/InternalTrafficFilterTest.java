package br.com.accenture.inventory.infrastructure.security.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class InternalTrafficFilterTest {

    private final InternalTrafficFilter filter = new InternalTrafficFilter();

    @Test
    void blocksRequestsWithoutInternalSecret() throws Exception {
        ReflectionTestUtils.setField(filter, "internalSecret", "test-secret");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("Acesso negado");
    }

    @Test
    void blocksRequestsWithInvalidInternalSecret() throws Exception {
        ReflectionTestUtils.setField(filter, "internalSecret", "test-secret");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Secret", "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Acesso negado");
    }

    @Test
    void allowsRequestsWithValidInternalSecret() throws Exception {
        ReflectionTestUtils.setField(filter, "internalSecret", "test-secret");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Secret", "test-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
