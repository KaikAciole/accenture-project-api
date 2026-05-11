package br.com.accenture.auth.infrastructure.security.adapter;

import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.service.TokenProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class JwtTokenProviderAdapter implements TokenProvider {

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration-minutes:60}")
    private long expirationMinutes;

    @Override
    public String generateAccessToken(UserCredential userCredential) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer("auth-microservice")
                    .withSubject(userCredential.getCustomerId().toString())
                    .withClaim("email", userCredential.getEmail().value())
                    .withClaim("roles", userCredential.getRoles().stream().map(Enum::name).toList())
                    .withExpiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES))
                    .sign(algorithm);

        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error while generating JWT token", exception);
        }
    }
}