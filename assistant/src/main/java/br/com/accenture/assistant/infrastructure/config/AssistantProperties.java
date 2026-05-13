package br.com.accenture.assistant.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant.gemini")
public record AssistantProperties(
        long chunkIdleTimeoutSeconds
) {

    public AssistantProperties {
        if (chunkIdleTimeoutSeconds <= 0) {
            chunkIdleTimeoutSeconds = 30;
        }
    }
}
