package com.portfolio.controlplane.domain.model;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FeatureFlag {

    private final @NonNull UUID id;
    private final @NonNull String key;
    private final @NonNull String description;
    private boolean enabled;
    private final @NonNull String environmentName;
    private final @NonNull List<@NonNull TargetingRule> rules;

    public FeatureFlag(
            @NonNull UUID id,
            @NonNull String key,
            @Nullable String description,
            boolean enabled,
            @NonNull String environmentName,
            @Nullable List<@NonNull TargetingRule> rules
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
        this.rules = copyRules(rules);
    }

    public static @NonNull FeatureFlag create(
            @NonNull UUID id,
            @NonNull String key,
            @Nullable String description,
            boolean enabled,
            @NonNull String environmentName
    ) {
        return new FeatureFlag(id, key, description, enabled, environmentName, List.of());
    }

    public void toggle(boolean enabled) {
        this.enabled = enabled;
    }

    public void addRule(@NonNull TargetingRule rule) {
        TargetingRule candidate = Objects.requireNonNull(rule, "Targeting rule must be provided");
        if (rules.contains(candidate)) {
            throw new IllegalArgumentException("This targeting rule already exists for the feature flag.");
        }
        this.rules.add(candidate);
    }

    public @NonNull TargetVersion evaluate(@NonNull Map<@NonNull String, @NonNull String> context) {
        if (!enabled) {
            return TargetVersion.STABLE;
        }

        if (rules.isEmpty()) {
            return TargetVersion.CANARY;
        }

        EnumMap<TargetVersion, List<@NonNull TargetingRule>> groupedRules = new EnumMap<>(TargetVersion.class);
        rules.forEach(rule -> {
            List<@NonNull TargetingRule> targetRules = groupedRules.computeIfAbsent(
                    rule.targetVersion(),
                    ignored -> new ArrayList<>()
            );
            targetRules.add(rule);
        });

        List<@NonNull TargetingRule> canaryRules = groupedRules.get(TargetVersion.CANARY);
        if (canaryRules != null
                && !canaryRules.isEmpty()
                && canaryRules.stream().allMatch(rule -> rule.matches(context))) {
            return TargetVersion.CANARY;
        }

        List<@NonNull TargetingRule> stableRules = groupedRules.get(TargetVersion.STABLE);
        if (stableRules != null
                && !stableRules.isEmpty()
                && stableRules.stream().allMatch(rule -> rule.matches(context))) {
            return TargetVersion.STABLE;
        }

        return TargetVersion.STABLE;
    }

    public @NonNull UUID getId() {
        return id;
    }

    public @NonNull String getKey() {
        return key;
    }

    public @NonNull String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public @NonNull String getEnvironmentName() {
        return environmentName;
    }

    public @NonNull List<@NonNull TargetingRule> getRules() {
        return List.copyOf(rules);
    }

    private static @NonNull List<@NonNull TargetingRule> copyRules(
            @Nullable List<@NonNull TargetingRule> source
    ) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }

        List<@NonNull TargetingRule> copy = new ArrayList<>();
        source.forEach(rule -> copy.add(Objects.requireNonNull(rule, "Targeting rule must not be null")));
        return copy;
    }
}
