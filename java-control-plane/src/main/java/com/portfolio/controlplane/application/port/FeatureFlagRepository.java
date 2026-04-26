package com.portfolio.controlplane.application.port;

import com.portfolio.controlplane.domain.model.FeatureFlag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository {

    FeatureFlag save(FeatureFlag flag);

    Optional<FeatureFlag> findById(UUID id);

    Optional<FeatureFlag> findByKey(String key);

    List<FeatureFlag> findAll();
}

