package com.portfolio.controlplane.application.query;

import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ListFeatureFlagsQuery {

    private final FeatureFlagRepository featureFlagRepository;

    public ListFeatureFlagsQuery(FeatureFlagRepository featureFlagRepository) {
        this.featureFlagRepository = featureFlagRepository;
    }

    @Transactional(readOnly = true)
    public List<FeatureFlag> execute() {
        return featureFlagRepository.findAll().stream()
                .sorted(Comparator.comparing(FeatureFlag::getEnvironmentName).thenComparing(FeatureFlag::getKey))
                .toList();
    }
}

