package com.portfolio.controlplane.domain.event;

import com.portfolio.controlplane.domain.model.FeatureFlag;
import com.portfolio.controlplane.domain.model.TargetingRule;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FlagChangedEvent(
        UUID flagId,
        String key,
        String description,
        boolean enabled,
        String environmentName,
        List<TargetingRule> rules,
        String reason,
        Instant changedAt
) {

    public static FlagChangedEvent from(FeatureFlag flag, String reason) {
        return new FlagChangedEvent(
                flag.getId(),
                flag.getKey(),
                flag.getDescription(),
                flag.isEnabled(),
                flag.getEnvironmentName(),
                flag.getRules(),
                reason,
                Instant.now()
        );
    }
}

