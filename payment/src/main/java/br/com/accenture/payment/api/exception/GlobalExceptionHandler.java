package br.com.accenture.payment.api.exception;

import br.com.accenture.payment.domain.exception.payment.DuplicatePaymentException;
import br.com.accenture.payment.domain.exception.payment.InvalidPaymentStatusException;
import br.com.accenture.payment.domain.exception.payment.PaymentNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handlePaymentNotFound(PaymentNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Payment not found");
        problemDetail.setDetail(exception.getMessage());
        problemDetail.setType(URI.create("/errors/payment-not-found"));

        return problemDetail;
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ProblemDetail handleDuplicatePayment(DuplicatePaymentException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Duplicate payment");
        problemDetail.setDetail(exception.getMessage());
        problemDetail.setType(URI.create("/errors/duplicate-payment"));

        return problemDetail;
    }

    @ExceptionHandler(InvalidPaymentStatusException.class)
    public ProblemDetail handleInvalidPaymentStatus(InvalidPaymentStatusException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        problemDetail.setTitle("Invalid payment status");
        problemDetail.setDetail(exception.getMessage());
        problemDetail.setType(URI.create("/errors/invalid-payment-status"));

        return problemDetail;
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Validation error");
        problemDetail.setDetail("One or more fields are invalid.");
        problemDetail.setType(URI.create("/errors/validation"));

        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid parameter");
        problemDetail.setDetail("Invalid value for parameter: " + exception.getName());
        problemDetail.setType(URI.create("/errors/invalid-parameter"));

        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid request");
        problemDetail.setDetail(exception.getMessage());
        problemDetail.setType(URI.create("/errors/invalid-request"));

        return problemDetail;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Payment update conflict");
        problemDetail.setDetail("The payment was updated by another transaction. Please try again.");
        problemDetail.setType(URI.create("/errors/payment-update-conflict"));

        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle("Internal server error");
        problemDetail.setDetail("An unexpected error occurred.");
        problemDetail.setType(URI.create("/errors/internal-server-error"));

        return problemDetail;
    }
}