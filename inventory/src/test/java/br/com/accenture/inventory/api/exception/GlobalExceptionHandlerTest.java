package br.com.accenture.inventory.api.exception;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.exception.DuplicateProductException;
import br.com.accenture.inventory.domain.exception.InsufficientStockException;
import br.com.accenture.inventory.domain.exception.InvalidReservationStatusException;
import br.com.accenture.inventory.domain.exception.ProductNotFoundException;
import br.com.accenture.inventory.domain.exception.StockReservationNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsDomainExceptionsToProblemDetails() {
        UUID id = UUID.randomUUID();

        assertProblem(handler.handleProductNotFound(new ProductNotFoundException(id)), HttpStatus.NOT_FOUND, "Product not found");
        assertThat(handler.handleProductNotFound(new ProductNotFoundException("SKU-001")).getDetail())
                .isEqualTo("Product not found with sku: SKU-001");
        assertProblem(handler.handleStockReservationNotFound(new StockReservationNotFoundException(id)), HttpStatus.NOT_FOUND, "Stock reservation not found");
        assertProblem(handler.handleDuplicateProduct(new DuplicateProductException("sku", "SKU-001")), HttpStatus.CONFLICT, "Duplicate product");
        assertProblem(handler.handleInsufficientStock(new InsufficientStockException("SKU-001", 2, 1)), HttpStatus.CONFLICT, "Insufficient stock");
        assertProblem(handler.handleInvalidReservationStatus(new InvalidReservationStatusException(ReservationStatus.CONFIRMED, "cancel")),
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Invalid reservation status");
    }

    @Test
    void mapsInfrastructureAndGenericExceptions() {
        assertProblem(handler.handleOptimisticLockingFailure(new OptimisticLockingFailureException("conflict")),
                HttpStatus.CONFLICT,
                "Stock update conflict");
        assertProblem(handler.handleGeneric(new RuntimeException("boom")),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error");
    }

    @Test
    void mapsConstraintViolationsToFieldErrors() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("create.request.sku");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("SKU is required");
        ConstraintViolation<?> flatViolation = mock(ConstraintViolation.class);
        Path flatPath = mock(Path.class);
        when(flatPath.toString()).thenReturn("name");
        when(flatViolation.getPropertyPath()).thenReturn(flatPath);
        when(flatViolation.getMessage()).thenReturn("Name is required");

        var problem = handler.handleConstraintViolation(new ConstraintViolationException(Set.of(violation, flatViolation)));

        assertProblem(problem, HttpStatus.BAD_REQUEST, "Validation error");
        assertThat(problem.getProperties().get("errors").toString()).contains("sku=SKU is required");
        assertThat(problem.getProperties().get("errors").toString()).contains("name=Name is required");
    }

    @Test
    void mapsMethodArgumentValidationToFieldErrors() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "productRequest");
        bindingResult.addError(new FieldError("productRequest", "sku", "SKU is required"));
        MethodParameter methodParameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("validationTarget", String.class),
                0
        );

        var problem = handler.handleValidation(new MethodArgumentNotValidException(methodParameter, bindingResult));

        assertProblem(problem, HttpStatus.BAD_REQUEST, "Validation error");
        assertThat(problem.getProperties().get("errors").toString()).contains("sku=SKU is required");
    }

    private static void assertProblem(org.springframework.http.ProblemDetail problem, HttpStatus status, String title) {
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getTitle()).isEqualTo(title);
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @SuppressWarnings("unused")
    private static void validationTarget(String value) {
    }
}
