package com.portfolio.controlplane.application.exception;

public class ExternalDependencyException extends RuntimeException {

    public ExternalDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalDependencyException(String message) {
        super(message);
    }
}

