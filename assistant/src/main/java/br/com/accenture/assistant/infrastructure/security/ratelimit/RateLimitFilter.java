package br.com.accenture.assistant.infrastructure.security.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@Order(2)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String USER_HEADER = "X-User-Id";
    private static final List<String> PROTECTED_PATTERNS = List.of("/assistant/**");

    private final BucketRegistry registry;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitFilter(BucketRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PROTECTED_PATTERNS.stream().noneMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(USER_HEADER);
        boolean userKey = userId != null && !userId.isBlank();
        String key = userKey ? "user:" + userId : "ip:" + request.getRemoteAddr();

        Bucket minuteBucket = registry.minuteBucketFor(key, userKey);
        Bucket dayBucket = registry.dayBucketFor(key, userKey);

        ConsumptionProbe minuteProbe = minuteBucket.tryConsumeAndReturnRemaining(1);
        if (!minuteProbe.isConsumed()) {
            writeRateLimitResponse(response, "Limite por minuto excedido", minuteProbe.getNanosToWaitForRefill());
            return;
        }

        ConsumptionProbe dayProbe = dayBucket.tryConsumeAndReturnRemaining(1);
        if (!dayProbe.isConsumed()) {
            writeRateLimitResponse(response, "Limite diário excedido", dayProbe.getNanosToWaitForRefill());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeRateLimitResponse(HttpServletResponse response, String detail, long nanosToWait) throws IOException {
        long retryAfterSeconds = Math.max(1, Duration.ofNanos(nanosToWait).toSeconds());
        log.warn("Rate limit hit: {} (retry in {}s)", detail, retryAfterSeconds);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String body = """
                {
                  "type": "about:blank",
                  "title": "Rate limit exceeded",
                  "status": 429,
                  "detail": "%s",
                  "retryAfterSeconds": %d,
                  "timestamp": "%s"
                }
                """.formatted(detail, retryAfterSeconds, Instant.now());
        response.getWriter().write(body);
    }
}
