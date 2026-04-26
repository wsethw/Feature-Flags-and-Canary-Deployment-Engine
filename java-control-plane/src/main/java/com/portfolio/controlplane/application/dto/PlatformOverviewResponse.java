package com.portfolio.controlplane.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PlatformOverviewResponse(
        Instant generatedAt,
        List<FeatureFlagResponse> flags,
        ServiceStatus controlPlane,
        ServiceStatus dataPlane,
        ServiceStatus guardian,
        ServiceStatus stable,
        ServiceStatus canary,
        Object guardianTelemetry
) {

    public record ServiceStatus(
            String name,
            String status,
            String baseUrl,
            boolean reachable,
            Map<String, Object> details
    ) {
    }
}

