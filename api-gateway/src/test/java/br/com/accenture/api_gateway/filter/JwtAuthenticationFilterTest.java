package br.com.accenture.api_gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-must-be-long-enough";

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
    }

    @Test
    void publicRouteShouldPassWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void publicGetProductsShouldPassWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void protectedRouteWithoutTokenShouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders/123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void customerCallingAdminRouteShouldReturn403() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = generateToken(customerId, List.of("CUSTOMER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/customers");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void adminCallingAdminRouteShouldPass() throws Exception {
        UUID adminId = UUID.randomUUID();
        String token = generateToken(adminId, List.of("ADMIN"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/customers");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, atLeastOnce()).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void wrappedRequestShouldExposeCustomerIdAndRolesHeaders() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = generateToken(customerId, List.of("ADMIN", "CUSTOMER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders/abc");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.captured).isNotNull();
        assertThat(chain.captured.getHeader("X-Customer-Id")).isEqualTo(customerId.toString());
        assertThat(chain.captured.getHeader("X-User-Roles")).isEqualTo("ADMIN,CUSTOMER");
    }

    @Test
    void customerCallingNonAdminRouteShouldPass() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = generateToken(customerId, List.of("CUSTOMER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders/my-orders");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, atLeastOnce()).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private static String generateToken(UUID subject, List<String> roles) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        return JWT.create()
                .withIssuer("auth-microservice")
                .withSubject(subject.toString())
                .withClaim("email", "user@example.com")
                .withClaim("roles", roles)
                .withExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .sign(algorithm);
    }

    private static <T> T any(Class<T> clazz) {
        return org.mockito.ArgumentMatchers.any(clazz);
    }

    private static class CapturingFilterChain implements FilterChain {
        HttpServletRequest captured;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            this.captured = (HttpServletRequest) request;
        }
    }
}
