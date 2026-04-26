package com.portfolio.controlplane.application.dto;

import java.util.Map;

public record DecisionPreviewResponse(
        String flagKey,
        String targetVersion,
        String reason,
        Map<String, String> context,
        boolean cacheHit
) {
}

