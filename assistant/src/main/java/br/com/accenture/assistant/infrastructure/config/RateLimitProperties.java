package br.com.accenture.assistant.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant.rate-limit")
public record RateLimitProperties(
        Limits user,
        Limits ip,
        Concurrency concurrency
) {

    public RateLimitProperties {
        if (user == null) user = new Limits(10, 100);
        if (ip == null) ip = new Limits(30, 300);
        if (concurrency == null) concurrency = new Concurrency(20);
    }

    public record Limits(long perMinute, long perDay) {
        public Limits {
            if (perMinute <= 0) perMinute = 1;
            if (perDay <= 0) perDay = 1;
        }
    }

    public record Concurrency(int maxConcurrentStreams) {
        public Concurrency {
            if (maxConcurrentStreams <= 0) maxConcurrentStreams = 1;
        }
    }
}
