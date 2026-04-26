package com.portfolio.controlplane.application.port;

import com.portfolio.controlplane.domain.model.FeatureFlag;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository {

    @NonNull FeatureFlag save(@NonNull FeatureFlag flag);

    @NonNull Optional<@NonNull FeatureFlag> findById(@NonNull UUID id);

    @NonNull Optional<@NonNull FeatureFlag> findByKey(@Nullable String key);

    @NonNull List<@NonNull FeatureFlag> findAll();
}
