package br.com.accenture.assistent.api.exception;

import br.com.accenture.assistent.domain.exception.AssistantAuthenticationException;
import br.com.accenture.assistent.domain.exception.AssistantRateLimitException;
import br.com.accenture.assistent.domain.exception.AssistantTimeoutException;
import br.com.accenture.assistent.domain.exception.AssistantUnavailableException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more fields"
        );
        problem.setTitle("Validation error");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.put(field, violation.getMessage());
        });

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more fields"
        );
        problem.setTitle("Validation error");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(AssistantRateLimitException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(AssistantRateLimitException ex) {
        log.warn("Assistant rate limit hit: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Assistente temporariamente indisponível por excesso de uso. Tente novamente em alguns segundos."
        );
        problem.setTitle("Rate limit exceeded");
        problem.setProperty("timestamp", Instant.now());
        if (ex.getRetryAfterSeconds() != null) {
            problem.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
        }

        HttpHeaders headers = new HttpHeaders();
        if (ex.getRetryAfterSeconds() != null) {
            headers.set(HttpHeaders.RETRY_AFTER, ex.getRetryAfterSeconds().toString());
        }
        return new ResponseEntity<>(problem, headers, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(AssistantTimeoutException.class)
    public ProblemDetail handleTimeout(AssistantTimeoutException ex) {
        log.error("Assistant call timed out", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.GATEWAY_TIMEOUT,
                "O assistente demorou muito para responder. Tente novamente."
        );
        problem.setTitle("Assistant timeout");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AssistantUnavailableException.class)
    public ProblemDetail handleUnavailable(AssistantUnavailableException ex) {
        log.error("Assistant provider unavailable", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "Falha ao se comunicar com o assistente. Tente novamente em instantes."
        );
        problem.setTitle("Assistant unavailable");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AssistantAuthenticationException.class)
    public ProblemDetail handleAuthentication(AssistantAuthenticationException ex) {
        log.error("Assistant provider authentication failure", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        problem.setTitle("Internal server error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        problem.setTitle("Internal server error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
