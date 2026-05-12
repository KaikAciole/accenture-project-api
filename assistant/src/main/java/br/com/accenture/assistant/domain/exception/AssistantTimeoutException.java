package br.com.accenture.assistant.domain.exception;

public class AssistantTimeoutException extends RuntimeException {

    public AssistantTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
