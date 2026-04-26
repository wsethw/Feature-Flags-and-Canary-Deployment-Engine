package com.portfolio.controlplane.application.dto;

import com.portfolio.controlplane.domain.model.FeatureFlag;
import com.portfolio.controlplane.domain.model.RuleOperator;
import com.portfolio.controlplane.domain.model.TargetVersion;
import org.eclipse.jdt.annotation.NonNull;

import java.util.List;
import java.util.UUID;

public record FeatureFlagResponse(
        @NonNull UUID id,
        @NonNull String key,
        @NonNull String description,
        boolean enabled,
        @NonNull String environmentName,
        @NonNull List<@NonNull TargetingRuleResponse> rules
) {

    public static @NonNull FeatureFlagResponse from(@NonNull FeatureFlag flag) {
        List<@NonNull TargetingRuleResponse> rules = flag.getRules().stream()
                .map(rule -> new TargetingRuleResponse(
                        rule.attribute(),
                        rule.operator(),
                        rule.value(),
                        rule.targetVersion()
                ))
                .toList();

        return new FeatureFlagResponse(
                flag.getId(),
                flag.getKey(),
                flag.getDescription(),
                flag.isEnabled(),
                flag.getEnvironmentName(),
                rules
        );
    }

    public record TargetingRuleResponse(
            @NonNull String attribute,
            @NonNull RuleOperator operator,
            @NonNull String value,
            @NonNull TargetVersion targetVersion
    ) {
    }
}
