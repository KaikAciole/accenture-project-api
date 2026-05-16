package br.com.accenture.order.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class InventoryUnavailableException extends RuntimeException {
    public InventoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
