package com.portfolio.controlplane.domain.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FeatureFlag {

    private final UUID id;
    private final String key;
    private final String description;
    private boolean enabled;
    private final String environmentName;
    private final List<TargetingRule> rules;

    public FeatureFlag(
            UUID id,
            String key,
            String description,
            boolean enabled,
            String environmentName,
            List<TargetingRule> rules
    ) {
        this.id = Objects.requireNonNull(id, "Flag id must be provided");
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Flag key must be provided");
        }
        if (environmentName == null || environmentName.isBlank()) {
            throw new IllegalArgumentException("Environment name must be provided");
        }

        this.key = key.trim().toLowerCase();
        this.description = description == null ? "" : description.trim();
        this.enabled = enabled;
        this.environmentName = environmentName.trim().toLowerCase();
        this.rules = new ArrayList<>(rules == null ? List.of() : rules);
    }

    public static FeatureFlag create(UUID id, String key, String description, boolean enabled, String environmentName) {
        return new FeatureFlag(id, key, description, enabled, environmentName, List.of());
    }

    public void toggle(boolean enabled) {
        this.enabled = enabled;
    }

    public void addRule(TargetingRule rule) {
        TargetingRule candidate = Objects.requireNonNull(rule, "Targeting rule must be provided");
        if (rules.contains(candidate)) {
            throw new IllegalArgumentException("This targeting rule already exists for the feature flag.");
        }
        this.rules.add(candidate);
    }

    public TargetVersion evaluate(Map<String, String> context) {
        if (!enabled) {
            return TargetVersion.STABLE;
        }

        if (rules.isEmpty()) {
            return TargetVersion.CANARY;
        }

        EnumMap<TargetVersion, List<TargetingRule>> groupedRules = new EnumMap<>(TargetVersion.class);
        rules.forEach(rule -> groupedRules.computeIfAbsent(rule.targetVersion(), ignored -> new ArrayList<>()).add(rule));

        List<TargetingRule> canaryRules = groupedRules.getOrDefault(TargetVersion.CANARY, List.of());
        if (!canaryRules.isEmpty() && canaryRules.stream().allMatch(rule -> rule.matches(context))) {
            return TargetVersion.CANARY;
        }

        List<TargetingRule> stableRules = groupedRules.getOrDefault(TargetVersion.STABLE, List.of());
        if (!stableRules.isEmpty() && stableRules.stream().allMatch(rule -> rule.matches(context))) {
            return TargetVersion.STABLE;
        }

        return TargetVersion.STABLE;
    }

    public UUID getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public List<TargetingRule> getRules() {
        return List.copyOf(rules);
    }
}
