package com.portfolio.controlplane.application.exception;

public class FeatureFlagAlreadyExistsException extends RuntimeException {

    public FeatureFlagAlreadyExistsException(String key) {
        super("A feature flag with key '%s' already exists.".formatted(key));
    }
}

