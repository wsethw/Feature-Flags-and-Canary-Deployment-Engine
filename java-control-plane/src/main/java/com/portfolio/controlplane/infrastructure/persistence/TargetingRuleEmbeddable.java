package com.portfolio.controlplane.infrastructure.persistence;

import com.portfolio.controlplane.domain.model.RuleOperator;
import com.portfolio.controlplane.domain.model.TargetVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.eclipse.jdt.annotation.NonNull;

import java.util.Objects;

@Embeddable
public class TargetingRuleEmbeddable {

    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attribute;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator_name", nullable = false, length = 20)
    private RuleOperator operator;

    @Column(name = "rule_value", nullable = false, length = 120)
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_version", nullable = false, length = 20)
    private TargetVersion targetVersion;

    protected TargetingRuleEmbeddable() {
    }

    public TargetingRuleEmbeddable(
            @NonNull String attribute,
            @NonNull RuleOperator operator,
            @NonNull String value,
            @NonNull TargetVersion targetVersion
    ) {
        this.attribute = Objects.requireNonNull(attribute, "Rule attribute must not be null");
        this.operator = Objects.requireNonNull(operator, "Rule operator must not be null");
        this.value = Objects.requireNonNull(value, "Rule value must not be null");
        this.targetVersion = Objects.requireNonNull(targetVersion, "Rule target version must not be null");
    }

    public @NonNull String getAttribute() {
        return requireLoaded(attribute, "attribute");
    }

    public @NonNull RuleOperator getOperator() {
        return requireLoaded(operator, "operator");
    }

    public @NonNull String getValue() {
        return requireLoaded(value, "value");
    }

    public @NonNull TargetVersion getTargetVersion() {
        return requireLoaded(targetVersion, "targetVersion");
    }

    private static <T> @NonNull T requireLoaded(T value, String fieldName) {
        return Objects.requireNonNull(value, "TargetingRuleEmbeddable." + fieldName + " must be loaded");
    }
}
