package br.com.accenture.assistant.infrastructure.security.ratelimit;

import br.com.accenture.assistant.infrastructure.config.RateLimitProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Component
public class ConcurrencyLimiter {

    private final Semaphore semaphore;

    public ConcurrencyLimiter(RateLimitProperties properties) {
        this.semaphore = new Semaphore(properties.concurrency().maxConcurrentStreams());
    }

    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    public void release() {
        semaphore.release();
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }
}
