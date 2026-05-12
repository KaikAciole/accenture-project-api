package br.com.accenture.assistant.infrastructure.integration.gemini;

import br.com.accenture.assistant.domain.exception.AssistantAuthenticationException;
import br.com.accenture.assistant.domain.exception.AssistantRateLimitException;
import br.com.accenture.assistant.domain.exception.AssistantTimeoutException;
import br.com.accenture.assistant.domain.exception.AssistantUnavailableException;
import br.com.accenture.assistant.domain.gateway.AssistantGateway;
import br.com.accenture.assistant.infrastructure.config.AssistantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiAssistantGateway implements AssistantGateway {

    private static final Pattern HTTP_CODE_PATTERN = Pattern.compile("^(\\d{3})\\b");
    private static final Pattern RETRY_AFTER_PATTERN = Pattern.compile("retry in (\\d+(?:\\.\\d+)?)s");

    private final ChatClient chatClient;
    private final AssistantProperties properties;

    @Override
    public Flux<String> ask(String question) {
        return chatClient.prompt()
                .user(question)
                .stream()
                .content()
                .timeout(Duration.ofSeconds(properties.chunkIdleTimeoutSeconds()))
                .onErrorMap(this::translate);
    }

    private RuntimeException translate(Throwable t) {
        if (t instanceof TimeoutException) {
            log.warn("Gemini stream idle for more than {}s", properties.chunkIdleTimeoutSeconds());
            return new AssistantTimeoutException("Gemini stream idle timeout", t);
        }

        Throwable target = (t.getCause() != null) ? t.getCause() : t;
        String message = target.getMessage() != null ? target.getMessage() : "";

        Integer code = extractHttpCode(message);
        if (code != null) {
            if (code == 429) {
                Long retryAfter = extractRetryAfter(message);
                log.warn("Gemini rate limit hit. Retry-After: {}s", retryAfter);
                return new AssistantRateLimitException(message, retryAfter, t);
            }
            if (code == 401 || code == 403) {
                log.error("Gemini authentication failure (HTTP {})", code);
                return new AssistantAuthenticationException(message, t);
            }
            if (code >= 500) {
                log.error("Gemini server error (HTTP {})", code);
                return new AssistantUnavailableException(message, t);
            }
        }

        log.error("Unexpected failure calling Gemini", t);
        return new AssistantUnavailableException(
                "Failed to call Gemini: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()),
                t
        );
    }

    private Integer extractHttpCode(String message) {
        Matcher m = HTTP_CODE_PATTERN.matcher(message);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private Long extractRetryAfter(String message) {
        Matcher m = RETRY_AFTER_PATTERN.matcher(message);
        if (m.find()) {
            double seconds = Double.parseDouble(m.group(1));
            return (long) Math.ceil(seconds);
        }
        return null;
    }
}
