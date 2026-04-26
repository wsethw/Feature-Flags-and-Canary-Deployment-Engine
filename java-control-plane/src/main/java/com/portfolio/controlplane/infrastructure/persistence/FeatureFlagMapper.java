package com.portfolio.controlplane.infrastructure.persistence;

import com.portfolio.controlplane.domain.model.FeatureFlag;
import com.portfolio.controlplane.domain.model.TargetingRule;
import org.eclipse.jdt.annotation.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FeatureFlagMapper {

    public @NonNull FeatureFlag toDomain(@NonNull FeatureFlagEntity entity) {
        List<@NonNull TargetingRule> rules = entity.getRules().stream()
                .map(rule -> new TargetingRule(
                        rule.getAttribute(),
                        rule.getOperator(),
                        rule.getValue(),
                        rule.getTargetVersion()
                ))
                .toList();

        return new FeatureFlag(
                entity.getId(),
                entity.getFlagKey(),
                entity.getDescription(),
                entity.isEnabled(),
                entity.getEnvironmentName(),
                rules
        );
    }

    public @NonNull FeatureFlagEntity toEntity(@NonNull FeatureFlag flag) {
        List<@NonNull TargetingRuleEmbeddable> rules = flag.getRules().stream()
                .map(rule -> new TargetingRuleEmbeddable(
                        rule.attribute(),
                        rule.operator(),
                        rule.value(),
                        rule.targetVersion()
                ))
                .toList();

        return new FeatureFlagEntity(
                flag.getId(),
                flag.getKey(),
                flag.getDescription(),
                flag.isEnabled(),
                flag.getEnvironmentName(),
                rules
        );
    }
}
