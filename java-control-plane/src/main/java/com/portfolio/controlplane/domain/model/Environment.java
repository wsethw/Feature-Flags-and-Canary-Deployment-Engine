package com.portfolio.controlplane.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Environment {

    private final UUID id;
    private final String name;
    private final List<FeatureFlag> flags;

    public Environment(UUID id, String name, List<FeatureFlag> flags) {
        this.id = Objects.requireNonNull(id, "Environment id must be provided");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Environment name must be provided");
        }
        this.name = name;
        this.flags = List.copyOf(flags == null ? List.of() : flags);
    }

    public boolean hasFlagKey(String key) {
        return flags.stream().anyMatch(flag -> flag.getKey().equalsIgnoreCase(key));
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<FeatureFlag> getFlags() {
        return flags;
    }
}

