package br.com.accenture.assistant.domain.exception;

public class AssistantConcurrencyLimitException extends RuntimeException {

    public AssistantConcurrencyLimitException(String message) {
        super(message);
    }
}
