package com.portfolio.controlplane.application.usecase;

import com.portfolio.controlplane.application.dto.FlagEvaluationResult;
import com.portfolio.controlplane.application.exception.FeatureFlagNotFoundException;
import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class EvaluateFlagUseCase {

    private final FeatureFlagRepository featureFlagRepository;

    public EvaluateFlagUseCase(FeatureFlagRepository featureFlagRepository) {
        this.featureFlagRepository = featureFlagRepository;
    }

    @Transactional(readOnly = true)
    public FlagEvaluationResult execute(String flagKey, Map<String, String> context) {
        FeatureFlag flag = featureFlagRepository.findByKey(flagKey)
                .orElseThrow(() -> new FeatureFlagNotFoundException(flagKey));

        return new FlagEvaluationResult(flag.getKey(), flag.evaluate(context));
    }
}
