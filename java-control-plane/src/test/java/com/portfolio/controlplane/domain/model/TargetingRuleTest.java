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
    void shouldUsePositiveFloorModuloForNegativeNumericBuckets() {
        TargetingRule ninetyNinePercent = new TargetingRule("userId", RuleOperator.PERCENTAGE, "99", TargetVersion.CANARY);
        TargetingRule oneHundredPercent = new TargetingRule("userId", RuleOperator.PERCENTAGE, "100", TargetVersion.CANARY);

        assertFalse(ninetyNinePercent.matches(Map.of("userId", "-1")));
        assertTrue(oneHundredPercent.matches(Map.of("userId", "-1")));
    }

    @Test
    void shouldUseJavaHashFloorModuloForTextBuckets() {
        TargetingRule fiftyTwoPercent = new TargetingRule("userId", RuleOperator.PERCENTAGE, "52", TargetVersion.CANARY);
        TargetingRule fiftyThreePercent = new TargetingRule("userId", RuleOperator.PERCENTAGE, "53", TargetVersion.CANARY);

        assertFalse(fiftyTwoPercent.matches(Map.of("userId", "polygenelubricants")));
        assertTrue(fiftyThreePercent.matches(Map.of("userId", "polygenelubricants")));
    }

    @Test
    void shouldHashNumericStringsOutsideLongRange() {
        TargetingRule threePercent = new TargetingRule("userId", RuleOperator.PERCENTAGE, "3", TargetVersion.CANARY);
        TargetingRule fourPercent = new TargetingRule("userId", RuleOperator.PERCENTAGE, "4", TargetVersion.CANARY);

        assertFalse(threePercent.matches(Map.of("userId", "9223372036854775808")));
        assertTrue(fourPercent.matches(Map.of("userId", "9223372036854775808")));
    }

    @Test
    void shouldRejectInvalidPercentageValue() {
        assertThrows(IllegalArgumentException.class, () ->
                new TargetingRule("userId", RuleOperator.PERCENTAGE, "abc", TargetVersion.CANARY)
        );
    }
}
