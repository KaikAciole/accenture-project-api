package br.com.accenture.assistant.infrastructure.security.ratelimit;

import br.com.accenture.assistant.infrastructure.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class BucketRegistry {

    private final ConcurrentMap<String, Bucket> minuteBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> dayBuckets = new ConcurrentHashMap<>();

    private final RateLimitProperties properties;

    public BucketRegistry(RateLimitProperties properties) {
        this.properties = properties;
    }

    public Bucket minuteBucketFor(String key, boolean userKey) {
        long capacity = userKey ? properties.user().perMinute() : properties.ip().perMinute();
        return minuteBuckets.computeIfAbsent(
                key,
                k -> Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(capacity)
                                .refillIntervally(capacity, Duration.ofMinutes(1))
                                .build())
                        .build()
        );
    }

    public Bucket dayBucketFor(String key, boolean userKey) {
        long capacity = userKey ? properties.user().perDay() : properties.ip().perDay();
        return dayBuckets.computeIfAbsent(
                key,
                k -> Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(capacity)
                                .refillIntervally(capacity, Duration.ofDays(1))
                                .build())
                        .build()
        );
    }
}
