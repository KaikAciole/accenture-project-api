package br.com.accenture.payment.api.exception;

import br.com.accenture.payment.domain.payment.enums.PaymentStatus;
import br.com.accenture.payment.domain.payment.exception.DuplicatePaymentException;
import br.com.accenture.payment.domain.payment.exception.InvalidPaymentStatusException;
import br.com.accenture.payment.domain.payment.exception.PaymentNotFoundException;
import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.exception.DuplicateWalletException;
import br.com.accenture.payment.domain.wallet.exception.InsufficientWalletBalanceException;
import br.com.accenture.payment.domain.wallet.exception.InvalidTopUpRequestException;
import br.com.accenture.payment.domain.wallet.exception.InvalidWalletTransactionException;
import br.com.accenture.payment.domain.wallet.exception.WalletNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.core.MethodParameter;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlePaymentNotFoundReturnsNotFound() {
        ProblemDetail problem = handler.handlePaymentNotFound(new PaymentNotFoundException(UUID.randomUUID()));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Payment not found");
        assertThat(problem.getType().toString()).isEqualTo("/errors/payment-not-found");
        assertThat(problem.getDetail()).contains("Payment not found");
    }

    @Test
    void handleDuplicatePaymentReturnsConflict() {
        ProblemDetail problem = handler.handleDuplicatePayment(new DuplicatePaymentException(UUID.randomUUID()));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Duplicate payment");
        assertThat(problem.getType().toString()).isEqualTo("/errors/duplicate-payment");
    }

    @Test
    void handleInvalidPaymentStatusReturnsUnprocessableEntity() {
        ProblemDetail problem = handler.handleInvalidPaymentStatus(
                new InvalidPaymentStatusException(PaymentStatus.PENDING, "approve")
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid payment status");
        assertThat(problem.getType().toString()).isEqualTo("/errors/invalid-payment-status");
    }

    @Test
    void handleWalletNotFoundReturnsNotFound() {
        ProblemDetail problem = handler.handleWalletNotFound(new WalletNotFoundException(UUID.randomUUID()));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Wallet not found");
        assertThat(problem.getType().toString()).isEqualTo("/errors/wallet-not-found");
    }

    @Test
    void handleDuplicateWalletReturnsConflict() {
        ProblemDetail problem = handler.handleDuplicateWallet(
                new DuplicateWalletException(UUID.randomUUID(), WalletOwnerType.CUSTOMER)
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Duplicate wallet");
        assertThat(problem.getType().toString()).isEqualTo("/errors/duplicate-wallet");
    }

    @Test
    void handleInsufficientWalletBalanceReturnsUnprocessableEntity() {
        ProblemDetail problem = handler.handleInsufficientWalletBalance(
                new InsufficientWalletBalanceException("no balance")
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
        assertThat(problem.getTitle()).isEqualTo("Insufficient wallet balance");
        assertThat(problem.getType().toString()).isEqualTo("/errors/insufficient-wallet-balance");
        assertThat(problem.getDetail()).isEqualTo("no balance");
    }

    @Test
    void handleInvalidWalletTransactionReturnsBadRequest() {
        ProblemDetail problem = handler.handleInvalidWalletTransaction(
                new InvalidWalletTransactionException("invalid")
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid wallet transaction");
        assertThat(problem.getType().toString()).isEqualTo("/errors/invalid-wallet-transaction");
    }

    @Test
    void handleInvalidTopUpRequestReturnsUnprocessableEntity() {
        ProblemDetail problem = handler.handleInvalidTopUpRequest(
                new InvalidTopUpRequestException("recusado")
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid top-up request");
        assertThat(problem.getType().toString()).isEqualTo("/errors/invalid-top-up-request");
        assertThat(problem.getDetail()).isEqualTo("recusado");
    }

    @Test
    void handleValidationReturnsBadRequestWithFieldErrors() throws NoSuchMethodException {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "amount", "must be greater than zero"));
        bindingResult.addError(new FieldError("request", "method", "must not be null"));
        MethodParameter parameter = new MethodParameter(
                DummyController.class.getDeclaredMethod("dummy", String.class),
                0
        );
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ProblemDetail problem = handler.handleValidation(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Validation error");
        assertThat(problem.getType().toString()).isEqualTo("/errors/validation");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors)
                .containsEntry("amount", "must be greater than zero")
                .containsEntry("method", "must not be null");
    }

    @Test
    void handleTypeMismatchReturnsBadRequest() throws NoSuchMethodException {
        MethodParameter parameter = new MethodParameter(
                DummyController.class.getDeclaredMethod("dummy", String.class),
                0
        );
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc",
                Integer.class,
                "page",
                parameter,
                new IllegalArgumentException("bad")
        );

        ProblemDetail problem = handler.handleTypeMismatch(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid parameter");
        assertThat(problem.getType().toString()).isEqualTo("/errors/invalid-parameter");
        assertThat(problem.getDetail()).contains("page");
    }

    @Test
    void handleConstraintViolationReturnsBadRequest() {
        ConstraintViolationException exception = new ConstraintViolationException("violation", Collections.emptySet());

        ProblemDetail problem = handler.handleConstraintViolation(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Constraint violation");
        assertThat(problem.getType().toString()).isEqualTo("/errors/constraint-violation");
    }

    @Test
    void handleIllegalArgumentReturnsBadRequest() {
        ProblemDetail problem = handler.handleIllegalArgument(new IllegalArgumentException("bad input"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid request");
        assertThat(problem.getType().toString()).isEqualTo("/errors/invalid-request");
        assertThat(problem.getDetail()).isEqualTo("bad input");
    }

    @Test
    void handleOptimisticLockingReturnsConflict() {
        ProblemDetail problem = handler.handleOptimisticLocking(new OptimisticLockingFailureException("boom"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Resource update conflict");
        assertThat(problem.getType().toString()).isEqualTo("/errors/resource-update-conflict");
    }

    @Test
    void handleUnexpectedReturnsInternalServerError() {
        ProblemDetail problem = handler.handleUnexpected(new RuntimeException("explosion"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal server error");
        assertThat(problem.getType().toString()).isEqualTo("/errors/internal-server-error");
    }

    private static final class DummyController {
        @SuppressWarnings("unused")
        void dummy(String value) {
        }
    }
}
