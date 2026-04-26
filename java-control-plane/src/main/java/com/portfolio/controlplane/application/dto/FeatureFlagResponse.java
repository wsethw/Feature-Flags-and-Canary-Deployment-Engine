package com.portfolio.controlplane.application.dto;

import com.portfolio.controlplane.domain.model.FeatureFlag;
import com.portfolio.controlplane.domain.model.RuleOperator;
import com.portfolio.controlplane.domain.model.TargetVersion;

import java.util.List;
import java.util.UUID;

public record FeatureFlagResponse(
        UUID id,
        String key,
        String description,
        boolean enabled,
        String environmentName,
        List<TargetingRuleResponse> rules
) {

    public static FeatureFlagResponse from(FeatureFlag flag) {
        return new FeatureFlagResponse(
                flag.getId(),
                flag.getKey(),
                flag.getDescription(),
                flag.isEnabled(),
                flag.getEnvironmentName(),
                flag.getRules().stream()
                        .map(rule -> new TargetingRuleResponse(
                                rule.attribute(),
                                rule.operator(),
                                rule.value(),
                                rule.targetVersion()
                        ))
                        .toList()
        );
    }

    public record TargetingRuleResponse(
            String attribute,
            RuleOperator operator,
            String value,
            TargetVersion targetVersion
    ) {
    }
}

