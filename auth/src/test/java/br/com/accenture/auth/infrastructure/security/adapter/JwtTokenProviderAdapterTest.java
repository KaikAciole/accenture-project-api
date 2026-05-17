package br.com.accenture.auth.infrastructure.security.adapter;

import br.com.accenture.auth.domain.enums.Role;
import br.com.accenture.auth.domain.model.UserCredential;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderAdapterTest {

    private JwtTokenProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JwtTokenProviderAdapter();
        ReflectionTestUtils.setField(adapter, "secret", "test-secret-key");
        ReflectionTestUtils.setField(adapter, "expirationMinutes", 60L);
    }

    @Test
    void generateAccessToken_shouldProduceVerifiableJwtWithExpectedClaims() {
        UUID customerId = UUID.randomUUID();
        UserCredential user = UserCredential.restore(
                UUID.randomUUID(), customerId, "user@example.com", "$2a$10$hashed",
                Set.of(Role.CUSTOMER, Role.ADMIN), true, Instant.now(), Instant.now()
        );

        String token = adapter.generateAccessToken(user);

        DecodedJWT decoded = JWT.require(Algorithm.HMAC256("test-secret-key"))
                .withIssuer("auth-microservice")
                .build()
                .verify(token);
        assertThat(decoded.getSubject()).isEqualTo(customerId.toString());
        assertThat(decoded.getClaim("email").asString()).isEqualTo("user@example.com");
        assertThat(decoded.getClaim("roles").asList(String.class))
                .containsExactlyInAnyOrder("CUSTOMER", "ADMIN");
        assertThat(decoded.getExpiresAt()).isAfter(java.util.Date.from(Instant.now()));
    }

    @Test
    void generateAccessToken_shouldWrapJwtCreationFailureInRuntimeException() {
        ReflectionTestUtils.setField(adapter, "secret", null);
        UserCredential user = UserCredential.restore(
                UUID.randomUUID(), UUID.randomUUID(), "user@example.com", "$2a$10$hashed",
                Set.of(Role.CUSTOMER), true, Instant.now(), Instant.now()
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.generateAccessToken(user))
                .isInstanceOf(RuntimeException.class);
    }
}
