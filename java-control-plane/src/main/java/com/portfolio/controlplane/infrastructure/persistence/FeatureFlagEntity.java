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

import java.util.ArrayList;
import java.util.List;
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
            UUID id,
            String flagKey,
            String description,
            boolean enabled,
            String environmentName,
            List<TargetingRuleEmbeddable> rules
    ) {
        this.id = id;
        this.flagKey = flagKey;
        this.description = description;
        this.enabled = enabled;
        this.environmentName = environmentName;
        this.rules = new ArrayList<>(rules);
    }

    public UUID getId() {
        return id;
    }

    public String getFlagKey() {
        return flagKey;
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

    public List<TargetingRuleEmbeddable> getRules() {
        return rules;
    }
}
