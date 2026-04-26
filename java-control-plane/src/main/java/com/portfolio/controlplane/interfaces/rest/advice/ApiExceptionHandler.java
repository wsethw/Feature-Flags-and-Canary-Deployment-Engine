package com.portfolio.controlplane.interfaces.rest.advice;

import com.portfolio.controlplane.application.exception.FeatureFlagAlreadyExistsException;
import com.portfolio.controlplane.application.exception.FeatureFlagNotFoundException;
import com.portfolio.controlplane.application.exception.ExternalDependencyException;
import com.portfolio.controlplane.infrastructure.security.RequestCorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
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
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(FeatureFlagAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            FeatureFlagAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), Map.of(), request);
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
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(ExternalDependencyException.class)
    public ResponseEntity<ApiErrorResponse> handleExternalDependencyFailure(
            ExternalDependencyException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "A required platform component is temporarily unavailable.",
                Map.of("dependency", exception.getMessage()),
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
            HttpStatus status,
            String message,
            Map<String, Object> details,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER),
                details
        ));
    }
}
