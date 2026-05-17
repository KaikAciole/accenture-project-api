package br.com.accenture.customer.api.exception;

import br.com.accenture.customer.domain.exception.AddressNotFoundException;
import br.com.accenture.customer.domain.exception.AuthSyncException;
import br.com.accenture.customer.domain.exception.CepLookupException;
import br.com.accenture.customer.domain.exception.CepNotFoundException;
import br.com.accenture.customer.domain.exception.CustomerNotFoundException;
import br.com.accenture.customer.domain.exception.DuplicateCustomerException;
import br.com.accenture.customer.domain.exception.DuplicateEmailInAuthException;
import br.com.accenture.customer.domain.exception.ImmutableFieldException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleCustomerNotFound_returns404() {
        UUID id = UUID.randomUUID();
        ProblemDetail problem = handler.handleCustomerNotFound(new CustomerNotFoundException(id));

        assertProblem(problem, HttpStatus.NOT_FOUND, "Customer not found");
        assertThat(problem.getDetail()).contains(id.toString());
    }

    @Test
    void handleAddressNotFound_returns404() {
        UUID id = UUID.randomUUID();
        ProblemDetail problem = handler.handleAddressNotFound(new AddressNotFoundException(id));

        assertProblem(problem, HttpStatus.NOT_FOUND, "Address not found");
        assertThat(problem.getDetail()).contains(id.toString());
    }

    @Test
    void handleCepNotFound_returns404() {
        ProblemDetail problem = handler.handleCepNotFound(new CepNotFoundException("00000000"));

        assertProblem(problem, HttpStatus.NOT_FOUND, "CEP not found");
        assertThat(problem.getDetail()).contains("00000000");
    }

    @Test
    void handleCepLookupError_returns502() {
        ProblemDetail problem = handler.handleCepLookupError(
                new CepLookupException("ViaCEP down", new RuntimeException())
        );

        assertProblem(problem, HttpStatus.BAD_GATEWAY, "CEP lookup error");
        assertThat(problem.getDetail()).isEqualTo("ViaCEP down");
    }

    @Test
    void handleDuplicateCustomer_returns409() {
        ProblemDetail problem = handler.handleDuplicateCustomer(
                new DuplicateCustomerException("email", "maria@example.com")
        );

        assertProblem(problem, HttpStatus.CONFLICT, "Duplicate customer");
        assertThat(problem.getDetail()).contains("email").contains("maria@example.com");
    }

    @Test
    void handleDuplicateEmailInAuth_returns409() {
        ProblemDetail problem = handler.handleDuplicateEmailInAuth(
                new DuplicateEmailInAuthException("maria@example.com")
        );

        assertProblem(problem, HttpStatus.CONFLICT, "Duplicate email");
        assertThat(problem.getDetail()).contains("maria@example.com");
    }

    @Test
    void handleAuthSync_returns502() {
        ProblemDetail problem = handler.handleAuthSync(
                new AuthSyncException("Auth service returned status 500")
        );

        assertProblem(problem, HttpStatus.BAD_GATEWAY, "Auth sync error");
        assertThat(problem.getDetail()).isEqualTo("Auth service returned status 500");
    }

    @Test
    void handleImmutableField_returns422() {
        ProblemDetail problem = handler.handleImmutableField(new ImmutableFieldException("cpf"));

        assertProblem(problem, HttpStatus.UNPROCESSABLE_ENTITY, "Immutable field");
        assertThat(problem.getDetail()).contains("cpf");
    }

    @Test
    void handleDataIntegrityViolation_returns409WithStandardMessage() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key", new RuntimeException("unique constraint violated")
        );

        ProblemDetail problem = handler.handleDataIntegrityViolation(ex);

        assertProblem(problem, HttpStatus.CONFLICT, "Data integrity conflict");
        assertThat(problem.getDetail()).contains("unicidade ou integridade");
    }

    @Test
    void handleNoResourceFound_returns404() throws NoResourceFoundException {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/missing", "GET");

        ProblemDetail problem = handler.handleNoResourceFound(ex);

        assertProblem(problem, HttpStatus.NOT_FOUND, "Resource not found");
    }

    @Test
    void handleValidation_returns400WithFieldErrors() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));
        bindingResult.addError(new FieldError("request", "cpf", "invalid format"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ProblemDetail problem = handler.handleValidation(ex);

        assertProblem(problem, HttpStatus.BAD_REQUEST, "Validation error");
        assertThat(problem.getDetail()).contains("Validation failed");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors)
                .containsEntry("email", "must not be blank")
                .containsEntry("cpf", "invalid format");
    }

    @Test
    void handleConstraintViolation_returns400AndStripsNestedPath() {
        ConstraintViolation<?> nested = mock(ConstraintViolation.class);
        Path nestedPath = mock(Path.class);
        when(nestedPath.toString()).thenReturn("create.arg0.email");
        when(nested.getPropertyPath()).thenReturn(nestedPath);
        when(nested.getMessage()).thenReturn("must not be blank");

        ConstraintViolation<?> simple = mock(ConstraintViolation.class);
        Path simplePath = mock(Path.class);
        when(simplePath.toString()).thenReturn("cpf");
        when(simple.getPropertyPath()).thenReturn(simplePath);
        when(simple.getMessage()).thenReturn("invalid format");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(nested, simple));

        ProblemDetail problem = handler.handleConstraintViolation(ex);

        assertProblem(problem, HttpStatus.BAD_REQUEST, "Validation error");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors)
                .containsEntry("email", "must not be blank")
                .containsEntry("cpf", "invalid format");
    }

    @Test
    void handleGeneric_returns500() {
        ProblemDetail problem = handler.handleGeneric(new RuntimeException("boom"));

        assertProblem(problem, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
    }

    private void assertProblem(ProblemDetail problem, HttpStatus status, String title) {
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getTitle()).isEqualTo(title);
        assertThat(problem.getProperties()).isNotNull();
        Object timestamp = problem.getProperties().get("timestamp");
        assertThat(timestamp).isInstanceOf(Instant.class);
    }
}
