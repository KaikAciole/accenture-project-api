package br.com.accenture.assistant.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant.gemini")
public record AssistantProperties(
        long timeoutSeconds
) {

    public AssistantProperties {
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 30;
        }
    }
}
