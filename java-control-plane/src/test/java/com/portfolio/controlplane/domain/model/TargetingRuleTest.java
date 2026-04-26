package com.portfolio.controlplane.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetingRuleTest {

    @Test
    void shouldMatchEqualsOperatorIgnoringCase() {
        TargetingRule rule = new TargetingRule("country", RuleOperator.EQUALS, "BR", TargetVersion.CANARY);

        assertTrue(rule.matches(Map.of("country", "br")));
    }

    @Test
    void shouldMatchPercentageBuckets() {
        TargetingRule rule = new TargetingRule("userId", RuleOperator.PERCENTAGE, "10", TargetVersion.CANARY);

        assertTrue(rule.matches(Map.of("userId", "7")));
        assertFalse(rule.matches(Map.of("userId", "15")));
    }

    @Test
    void shouldRejectInvalidPercentageValue() {
        assertThrows(IllegalArgumentException.class, () ->
                new TargetingRule("userId", RuleOperator.PERCENTAGE, "abc", TargetVersion.CANARY)
        );
    }
}

