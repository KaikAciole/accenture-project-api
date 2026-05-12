package br.com.accenture.assistant.domain.exception;

public class AssistantRateLimitException extends RuntimeException {

    private final Long retryAfterSeconds;

    public AssistantRateLimitException(String message, Long retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
