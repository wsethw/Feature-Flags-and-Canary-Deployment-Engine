package com.portfolio.controlplane.infrastructure.persistence;

import com.portfolio.controlplane.domain.model.RuleOperator;
import com.portfolio.controlplane.domain.model.TargetVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

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

    public TargetingRuleEmbeddable(String attribute, RuleOperator operator, String value, TargetVersion targetVersion) {
        this.attribute = attribute;
        this.operator = operator;
        this.value = value;
        this.targetVersion = targetVersion;
    }

    public String getAttribute() {
        return attribute;
    }

    public RuleOperator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public TargetVersion getTargetVersion() {
        return targetVersion;
    }
}
