package com.portfolio.controlplane.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeatureFlagTest {

    @Test
    void shouldRouteMatchingContextToCanary() {
        FeatureFlag flag = FeatureFlag.create(
                UUID.randomUUID(),
                "new-checkout",
                "Routes users to the canary checkout",
                true,
                "production"
        );

        flag.addRule(new TargetingRule("country", RuleOperator.EQUALS, "BR", TargetVersion.CANARY));
        flag.addRule(new TargetingRule("platform", RuleOperator.EQUALS, "iOS", TargetVersion.CANARY));
        flag.addRule(new TargetingRule("userId", RuleOperator.PERCENTAGE, "10", TargetVersion.CANARY));

        assertEquals(TargetVersion.CANARY, flag.evaluate(Map.of(
                "country", "BR",
                "platform", "iOS",
                "userId", "7"
        )));
    }

    @Test
    void shouldRejectDuplicateRules() {
        FeatureFlag flag = FeatureFlag.create(
                UUID.randomUUID(),
                "new-checkout",
                "Routes users to the canary checkout",
                true,
                "production"
        );

        TargetingRule rule = new TargetingRule("country", RuleOperator.EQUALS, "BR", TargetVersion.CANARY);
        flag.addRule(rule);

        assertThrows(IllegalArgumentException.class, () -> flag.addRule(rule));
    }
}
