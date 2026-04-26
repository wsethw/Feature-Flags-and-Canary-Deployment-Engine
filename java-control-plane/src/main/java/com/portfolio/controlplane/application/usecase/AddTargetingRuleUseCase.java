package com.portfolio.controlplane.application.usecase;

import com.portfolio.controlplane.application.dto.AddTargetingRuleRequest;
import com.portfolio.controlplane.application.exception.FeatureFlagNotFoundException;
import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.application.port.FlagChangePublisher;
import com.portfolio.controlplane.domain.event.FlagChangedEvent;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import com.portfolio.controlplane.domain.model.TargetingRule;
import org.eclipse.jdt.annotation.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
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
    public @NonNull FeatureFlag execute(@NonNull UUID flagId, @NonNull AddTargetingRuleRequest request) {
        UUID safeFlagId = Objects.requireNonNull(flagId);
        AddTargetingRuleRequest safeRequest = Objects.requireNonNull(request);
        FeatureFlag flag = featureFlagRepository.findById(safeFlagId)
                .orElseThrow(() -> new FeatureFlagNotFoundException(safeFlagId));

        flag.addRule(new TargetingRule(
                safeRequest.attribute(),
                safeRequest.operator(),
                safeRequest.value(),
                safeRequest.targetVersion()
        ));

        FeatureFlag updatedFlag = featureFlagRepository.save(flag);
        flagChangePublisher.publish(FlagChangedEvent.from(updatedFlag, "rule-added"));

        return updatedFlag;
    }
}
