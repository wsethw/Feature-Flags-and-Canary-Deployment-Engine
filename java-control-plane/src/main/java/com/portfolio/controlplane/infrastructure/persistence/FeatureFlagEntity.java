package com.portfolio.controlplane.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.eclipse.jdt.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "feature_flags")
public class FeatureFlagEntity {

    @Id
    private UUID id;

    @Column(name = "flag_key", nullable = false, unique = true, length = 80)
    private String flagKey;

    @Column(name = "description", nullable = false, length = 280)
    private String description;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "environment_name", nullable = false, length = 40)
    private String environmentName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "targeting_rules", joinColumns = @JoinColumn(name = "feature_flag_id"))
    @OrderColumn(name = "rule_order")
    private List<TargetingRuleEmbeddable> rules = new ArrayList<>();

    protected FeatureFlagEntity() {
    }

    public FeatureFlagEntity(
            @NonNull UUID id,
            @NonNull String flagKey,
            @NonNull String description,
            boolean enabled,
            @NonNull String environmentName,
            @NonNull List<@NonNull TargetingRuleEmbeddable> rules
    ) {
        this.id = Objects.requireNonNull(id, "Feature flag entity id must not be null");
        this.flagKey = Objects.requireNonNull(flagKey, "Feature flag entity key must not be null");
        this.description = Objects.requireNonNull(description, "Feature flag entity description must not be null");
        this.enabled = enabled;
        this.environmentName = Objects.requireNonNull(environmentName, "Feature flag entity environment must not be null");
        this.rules = new ArrayList<>(rules);
    }

    public @NonNull UUID getId() {
        return requireLoaded(id, "id");
    }

    public @NonNull String getFlagKey() {
        return requireLoaded(flagKey, "flagKey");
    }

    public @NonNull String getDescription() {
        return requireLoaded(description, "description");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public @NonNull String getEnvironmentName() {
        return requireLoaded(environmentName, "environmentName");
    }

    public @NonNull List<@NonNull TargetingRuleEmbeddable> getRules() {
        List<TargetingRuleEmbeddable> loadedRules = rules == null ? List.of() : rules;
        List<@NonNull TargetingRuleEmbeddable> safeRules = new ArrayList<>();
        for (TargetingRuleEmbeddable rule : loadedRules) {
            safeRules.add(requireLoaded(rule, "rules[]"));
        }
        return List.copyOf(safeRules);
    }

    private static <T> @NonNull T requireLoaded(T value, String fieldName) {
        return Objects.requireNonNull(value, "FeatureFlagEntity." + fieldName + " must be loaded");
    }
}
