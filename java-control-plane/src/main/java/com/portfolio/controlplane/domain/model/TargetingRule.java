package com.portfolio.controlplane.domain.model;

import org.eclipse.jdt.annotation.NonNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public record TargetingRule(
        @NonNull String attribute,
        @NonNull RuleOperator operator,
        @NonNull String value,
        @NonNull TargetVersion targetVersion
) {

    public TargetingRule {
        if (attribute == null || attribute.isBlank()) {
            throw new IllegalArgumentException("Rule attribute must be provided");
        }
        Objects.requireNonNull(operator, "Rule operator must be provided");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Rule value must be provided");
        }
        Objects.requireNonNull(targetVersion, "Rule target version must be provided");

        attribute = attribute.trim();
        value = value.trim();

        if (operator == RuleOperator.PERCENTAGE) {
            int percentage;
            try {
                percentage = Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Percentage rules must use a numeric value between 0 and 100.");
            }
            if (percentage < 0 || percentage > 100) {
                throw new IllegalArgumentException("Percentage must be between 0 and 100");
            }
        }
    }

    public boolean matches(@NonNull Map<@NonNull String, @NonNull String> context) {
        String candidate = context.get(attribute);
        if (candidate == null || candidate.isBlank()) {
            return false;
        }

        return switch (operator) {
            case EQUALS -> candidate.trim().equalsIgnoreCase(value.trim());
            case IN -> Arrays.stream(value.split(","))
                    .map(String::trim)
                    .anyMatch(option -> option.equalsIgnoreCase(candidate.trim()));
            case PERCENTAGE -> stableBucket(candidate.trim()) < Integer.parseInt(value);
        };
    }

    private int stableBucket(String candidate) {
        try {
            long numeric = Long.parseLong(candidate);
            return Math.floorMod((int) (numeric % 100), 100);
        } catch (NumberFormatException ignored) {
            return Math.floorMod(candidate.hashCode(), 100);
        }
    }
}
