package com.portfolio.controlplane.application.query;

import com.portfolio.controlplane.application.exception.FeatureFlagNotFoundException;
import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import org.eclipse.jdt.annotation.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class GetFeatureFlagQuery {

    private final FeatureFlagRepository featureFlagRepository;

    public GetFeatureFlagQuery(FeatureFlagRepository featureFlagRepository) {
        this.featureFlagRepository = featureFlagRepository;
    }

    @Transactional(readOnly = true)
    public @NonNull FeatureFlag byId(@NonNull UUID flagId) {
        UUID safeFlagId = Objects.requireNonNull(flagId);
        return featureFlagRepository.findById(safeFlagId)
                .orElseThrow(() -> new FeatureFlagNotFoundException(safeFlagId));
    }

    @Transactional(readOnly = true)
    public @NonNull FeatureFlag byKey(@NonNull String key) {
        String safeKey = Objects.requireNonNull(key);
        return featureFlagRepository.findByKey(safeKey)
                .orElseThrow(() -> new FeatureFlagNotFoundException(safeKey));
    }
}
