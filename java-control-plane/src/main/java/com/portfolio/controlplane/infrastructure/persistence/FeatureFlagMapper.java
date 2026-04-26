package com.portfolio.controlplane.infrastructure.persistence;

import com.portfolio.controlplane.domain.model.FeatureFlag;
import com.portfolio.controlplane.domain.model.TargetingRule;
import org.springframework.stereotype.Component;

@Component
public class FeatureFlagMapper {

    public FeatureFlag toDomain(FeatureFlagEntity entity) {
        return new FeatureFlag(
                entity.getId(),
                entity.getFlagKey(),
                entity.getDescription(),
                entity.isEnabled(),
                entity.getEnvironmentName(),
                entity.getRules().stream()
                        .map(rule -> new TargetingRule(
                                rule.getAttribute(),
                                rule.getOperator(),
                                rule.getValue(),
                                rule.getTargetVersion()
                        ))
                        .toList()
        );
    }

    public FeatureFlagEntity toEntity(FeatureFlag flag) {
        return new FeatureFlagEntity(
                flag.getId(),
                flag.getKey(),
                flag.getDescription(),
                flag.isEnabled(),
                flag.getEnvironmentName(),
                flag.getRules().stream()
                        .map(rule -> new TargetingRuleEmbeddable(
                                rule.attribute(),
                                rule.operator(),
                                rule.value(),
                                rule.targetVersion()
                        ))
                        .toList()
        );
    }
}

