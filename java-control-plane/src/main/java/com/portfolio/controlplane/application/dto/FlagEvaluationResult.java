package com.portfolio.controlplane.application.dto;

import com.portfolio.controlplane.domain.model.TargetVersion;

public record FlagEvaluationResult(
        String flagKey,
        TargetVersion targetVersion
) {
}

