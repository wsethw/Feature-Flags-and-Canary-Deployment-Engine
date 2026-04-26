package com.portfolio.controlplane.application.query;

import com.portfolio.controlplane.application.exception.FeatureFlagNotFoundException;
import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetFeatureFlagQuery {

    private final FeatureFlagRepository featureFlagRepository;

    public GetFeatureFlagQuery(FeatureFlagRepository featureFlagRepository) {
        this.featureFlagRepository = featureFlagRepository;
    }

    @Transactional(readOnly = true)
    public FeatureFlag byId(UUID flagId) {
        return featureFlagRepository.findById(flagId)
                .orElseThrow(() -> new FeatureFlagNotFoundException(flagId));
    }

    @Transactional(readOnly = true)
    public FeatureFlag byKey(String key) {
        return featureFlagRepository.findByKey(key)
                .orElseThrow(() -> new FeatureFlagNotFoundException(key));
    }
}

