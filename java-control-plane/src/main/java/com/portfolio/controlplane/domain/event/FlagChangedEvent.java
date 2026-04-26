package com.portfolio.controlplane.domain.event;

import com.portfolio.controlplane.domain.model.FeatureFlag;
import com.portfolio.controlplane.domain.model.TargetingRule;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FlagChangedEvent(
        @NonNull UUID flagId,
        @NonNull String key,
        @NonNull String description,
        boolean enabled,
        @NonNull String environmentName,
        @NonNull List<@NonNull TargetingRule> rules,
        @NonNull String reason,
        @NonNull Instant changedAt
) {

    public static @NonNull FlagChangedEvent from(@NonNull FeatureFlag flag, @Nullable String reason) {
        String normalizedReason = reason == null || reason.isBlank() ? "flag-changed" : reason.trim();
        return new FlagChangedEvent(
                flag.getId(),
                flag.getKey(),
                flag.getDescription(),
                flag.isEnabled(),
                flag.getEnvironmentName(),
                flag.getRules(),
                normalizedReason,
                Instant.now()
        );
    }
}
