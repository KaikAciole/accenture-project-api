package br.com.accenture.assistant.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant.gemini")
public record AssistantProperties(
        long chunkIdleTimeoutSeconds,
        long totalStreamTimeoutSeconds,
        int maxChunksPerStream
) {

    public AssistantProperties {
        if (chunkIdleTimeoutSeconds <= 0) {
            chunkIdleTimeoutSeconds = 30;
        }
        if (totalStreamTimeoutSeconds <= 0) {
            totalStreamTimeoutSeconds = 60;
        }
        if (maxChunksPerStream <= 0) {
            maxChunksPerStream = 200;
        }
    }
}
