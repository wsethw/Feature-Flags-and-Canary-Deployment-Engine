package com.portfolio.controlplane.interfaces.rest.advice;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String requestId,
        Map<String, Object> details
) {
}

