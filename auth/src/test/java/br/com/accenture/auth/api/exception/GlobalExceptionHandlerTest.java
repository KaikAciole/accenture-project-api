package br.com.accenture.auth.api.exception;

import br.com.accenture.auth.application.exception.CredentialNotFoundException;
import br.com.accenture.auth.application.exception.InvalidCredentialsException;
import br.com.accenture.auth.application.exception.InvalidPasswordResetTokenException;
import br.com.accenture.auth.application.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationExceptions_shouldReturnBadRequestWithFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must be a valid email"));
        bindingResult.addError(new FieldError("request", "password", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ProblemDetail problem = handler.handleValidationExceptions(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getDetail()).contains("validation errors");
        assertThat(problem.getProperties()).containsKey("timestamp");
        @SuppressWarnings("unchecked")
        Map<String, String> invalidParams = (Map<String, String>) problem.getProperties().get("invalid_params");
        assertThat(invalidParams).containsEntry("email", "must be a valid email")
                .containsEntry("password", "must not be blank");
    }

    @Test
    void handleUserAlreadyExistsException_shouldReturnConflict() {
        ProblemDetail problem = handler.handleUserAlreadyExistsException(new UserAlreadyExistsException("duplicate"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("User Already Exists");
        assertThat(problem.getDetail()).isEqualTo("duplicate");
    }

    @Test
    void handleInvalidCredentialsException_shouldReturnUnauthorized() {
        ProblemDetail problem = handler.handleInvalidCredentialsException(new InvalidCredentialsException("bad creds"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problem.getTitle()).isEqualTo("Unauthorized");
        assertThat(problem.getDetail()).isEqualTo("bad creds");
    }

    @Test
    void handleCredentialNotFoundException_shouldReturnNotFound() {
        ProblemDetail problem = handler.handleCredentialNotFoundException(new CredentialNotFoundException("missing"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Credential Not Found");
    }

    @Test
    void handleInvalidPasswordResetTokenException_shouldReturnUnprocessableEntity() {
        ProblemDetail problem = handler.handleInvalidPasswordResetTokenException(
                new InvalidPasswordResetTokenException("invalid token"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Password Reset Token");
    }

    @Test
    void handleDomainExceptions_shouldReturnBadRequestForIllegalArgument() {
        ProblemDetail problem = handler.handleDomainExceptions(new IllegalArgumentException("bad input"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Domain Rule Violation");
        assertThat(problem.getDetail()).isEqualTo("bad input");
    }

    @Test
    void handleDomainExceptions_shouldReturnBadRequestForIllegalState() {
        ProblemDetail problem = handler.handleDomainExceptions(new IllegalStateException("bad state"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("bad state");
    }

    @Test
    void handleAllOtherExceptions_shouldReturnInternalServerError() {
        ProblemDetail problem = handler.handleAllOtherExceptions(new RuntimeException("boom"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).contains("unexpected error");
    }
}
