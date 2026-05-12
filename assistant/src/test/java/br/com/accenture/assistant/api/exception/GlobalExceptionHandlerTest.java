package br.com.accenture.assistant.api.exception;

import br.com.accenture.assistant.domain.exception.AssistantAuthenticationException;
import br.com.accenture.assistant.domain.exception.AssistantRateLimitException;
import br.com.accenture.assistant.domain.exception.AssistantTimeoutException;
import br.com.accenture.assistant.domain.exception.AssistantUnavailableException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRateLimit_shouldReturn503WithRetryAfterHeader() {
        AssistantRateLimitException ex = new AssistantRateLimitException(
                "rate limit hit", 18L, new RuntimeException()
        );

        ResponseEntity<ProblemDetail> response = handler.handleRateLimit(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("18");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Rate limit exceeded");
        assertThat(response.getBody().getProperties()).containsEntry("retryAfterSeconds", 18L);
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }

    @Test
    void handleRateLimit_withoutRetryAfter_shouldOmitHeader() {
        AssistantRateLimitException ex = new AssistantRateLimitException(
                "rate limit hit", null, new RuntimeException()
        );

        ResponseEntity<ProblemDetail> response = handler.handleRateLimit(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).doesNotContainKey("retryAfterSeconds");
    }

    @Test
    void handleTimeout_shouldReturn504() {
        AssistantTimeoutException ex = new AssistantTimeoutException("timeout", new RuntimeException());

        ProblemDetail problem = handler.handleTimeout(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
        assertThat(problem.getTitle()).isEqualTo("Assistant timeout");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    void handleUnavailable_shouldReturn502() {
        AssistantUnavailableException ex = new AssistantUnavailableException("unavailable", new RuntimeException());

        ProblemDetail problem = handler.handleUnavailable(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(problem.getTitle()).isEqualTo("Assistant unavailable");
    }

    @Test
    void handleAuthentication_shouldReturn500WithGenericMessage() {
        AssistantAuthenticationException ex = new AssistantAuthenticationException("auth failed", new RuntimeException());

        ProblemDetail problem = handler.handleAuthentication(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal server error");
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void handleGeneric_shouldReturn500() {
        Exception ex = new RuntimeException("boom");

        ProblemDetail problem = handler.handleGeneric(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal server error");
    }

    @Test
    void handleValidation_shouldReturn400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("askRequest", "question", "must not be blank");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ProblemDetail problem = handler.handleValidation(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Validation error");
        @SuppressWarnings("unchecked")
        var errors = (java.util.Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors).containsEntry("question", "must not be blank");
    }

    @Test
    void handleConstraintViolation_shouldReturn400WithFieldErrors() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("ask.question");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ProblemDetail problem = handler.handleConstraintViolation(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Validation error");
        @SuppressWarnings("unchecked")
        var errors = (java.util.Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors).containsEntry("question", "must not be blank");
    }
}
