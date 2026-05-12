package br.com.accenture.assistant.domain.exception;

public class AssistantAuthenticationException extends RuntimeException {

    public AssistantAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
