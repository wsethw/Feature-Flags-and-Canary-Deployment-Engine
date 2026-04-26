package com.portfolio.controlplane.interfaces.rest.advice;

import com.portfolio.controlplane.application.exception.FeatureFlagAlreadyExistsException;
import com.portfolio.controlplane.application.exception.FeatureFlagNotFoundException;
import com.portfolio.controlplane.application.exception.ExternalDependencyException;
import com.portfolio.controlplane.infrastructure.security.RequestCorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jdt.annotation.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(FeatureFlagNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            FeatureFlagNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, safeMessage(exception, "Feature flag was not found."), Map.of(), request);
    }

    @ExceptionHandler(FeatureFlagAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            FeatureFlagAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, safeMessage(exception, "Feature flag already exists."), Map.of(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "The request conflicts with existing persisted state.",
                Map.of("exception", exception.getClass().getSimpleName()),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> details = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage(),
                        (left, right) -> right
                ));

        return buildResponse(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.", details, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, safeMessage(exception, "Invalid request."), Map.of(), request);
    }

    @ExceptionHandler(ExternalDependencyException.class)
    public ResponseEntity<ApiErrorResponse> handleExternalDependencyFailure(
            ExternalDependencyException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "A required platform component is temporarily unavailable.",
                Map.of("dependency", safeMessage(exception, "External dependency failure")),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandled(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The control plane could not complete the request right now.",
                Map.of("exception", exception.getClass().getSimpleName()),
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            @NonNull HttpStatus status,
            @NonNull String message,
            @NonNull Map<String, Object> details,
            @NonNull HttpServletRequest request
    ) {
        int statusCode = status.value();
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                statusCode,
                status.getReasonPhrase(),
                message,
                RequestCorrelationFilter.getRequestId(request),
                details
        );
        return ResponseEntity.status(statusCode).body(body);
    }

    private static @NonNull String safeMessage(@NonNull Exception exception, @NonNull String fallback) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
