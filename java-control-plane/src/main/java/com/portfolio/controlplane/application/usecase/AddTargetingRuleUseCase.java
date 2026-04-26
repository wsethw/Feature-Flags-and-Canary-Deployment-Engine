package com.portfolio.controlplane.application.usecase;

import com.portfolio.controlplane.application.dto.AddTargetingRuleRequest;
import com.portfolio.controlplane.application.exception.FeatureFlagNotFoundException;
import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.application.port.FlagChangePublisher;
import com.portfolio.controlplane.domain.event.FlagChangedEvent;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import com.portfolio.controlplane.domain.model.TargetingRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AddTargetingRuleUseCase {

    private final FeatureFlagRepository featureFlagRepository;
    private final FlagChangePublisher flagChangePublisher;

    public AddTargetingRuleUseCase(
            FeatureFlagRepository featureFlagRepository,
            FlagChangePublisher flagChangePublisher
    ) {
        this.featureFlagRepository = featureFlagRepository;
        this.flagChangePublisher = flagChangePublisher;
    }

    @Transactional
    public FeatureFlag execute(UUID flagId, AddTargetingRuleRequest request) {
        FeatureFlag flag = featureFlagRepository.findById(flagId)
                .orElseThrow(() -> new FeatureFlagNotFoundException(flagId));

        flag.addRule(new TargetingRule(
                request.attribute(),
                request.operator(),
                request.value(),
                request.targetVersion()
        ));

        FeatureFlag updatedFlag = featureFlagRepository.save(flag);
        flagChangePublisher.publish(FlagChangedEvent.from(updatedFlag, "rule-added"));

        return updatedFlag;
    }
}
