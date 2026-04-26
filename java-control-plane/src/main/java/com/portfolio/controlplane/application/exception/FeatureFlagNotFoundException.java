package com.portfolio.controlplane.application.exception;

import java.util.UUID;

public class FeatureFlagNotFoundException extends RuntimeException {

    public FeatureFlagNotFoundException(UUID flagId) {
        super("Feature flag not found for id '%s'.".formatted(flagId));
    }

    public FeatureFlagNotFoundException(String key) {
        super("Feature flag not found for key '%s'.".formatted(key));
    }
}

