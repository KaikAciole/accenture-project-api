package br.com.accenture.assistant.infrastructure.security.ratelimit;

import br.com.accenture.assistant.infrastructure.config.RateLimitProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrencyLimiterTest {

    private ConcurrencyLimiter newLimiterWithMax(int maxConcurrentStreams) {
        RateLimitProperties properties = new RateLimitProperties(
                new RateLimitProperties.Limits(10, 100),
                new RateLimitProperties.Limits(30, 300),
                new RateLimitProperties.Concurrency(maxConcurrentStreams)
        );
        return new ConcurrencyLimiter(properties);
    }

    @Test
    void tryAcquire_shouldSucceedUpToMaxConcurrentStreams() {
        ConcurrencyLimiter limiter = newLimiterWithMax(3);

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
    }

    @Test
    void tryAcquire_shouldFailWhenLimitReached() {
        ConcurrencyLimiter limiter = newLimiterWithMax(2);

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();
    }

    @Test
    void release_shouldAllowSubsequentAcquireAfterLimitReached() {
        ConcurrencyLimiter limiter = newLimiterWithMax(1);

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();

        limiter.release();

        assertThat(limiter.tryAcquire()).isTrue();
    }

    @Test
    void availablePermits_shouldReflectAcquireAndReleaseOperations() {
        ConcurrencyLimiter limiter = newLimiterWithMax(3);
        assertThat(limiter.availablePermits()).isEqualTo(3);

        limiter.tryAcquire();
        assertThat(limiter.availablePermits()).isEqualTo(2);

        limiter.tryAcquire();
        assertThat(limiter.availablePermits()).isEqualTo(1);

        limiter.release();
        assertThat(limiter.availablePermits()).isEqualTo(2);
    }
}
