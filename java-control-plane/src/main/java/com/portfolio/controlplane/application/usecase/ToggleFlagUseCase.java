package com.portfolio.controlplane.application.usecase;

import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.application.port.FlagChangePublisher;
import com.portfolio.controlplane.application.exception.FeatureFlagNotFoundException;
import com.portfolio.controlplane.domain.event.FlagChangedEvent;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ToggleFlagUseCase {

    private final FeatureFlagRepository featureFlagRepository;
    private final FlagChangePublisher flagChangePublisher;

    public ToggleFlagUseCase(
            FeatureFlagRepository featureFlagRepository,
            FlagChangePublisher flagChangePublisher
    ) {
        this.featureFlagRepository = featureFlagRepository;
        this.flagChangePublisher = flagChangePublisher;
    }

    @Transactional
    public FeatureFlag execute(UUID flagId, boolean enabled, String reason) {
        FeatureFlag flag = featureFlagRepository.findById(flagId)
                .orElseThrow(() -> new FeatureFlagNotFoundException(flagId));

        flag.toggle(enabled);

        FeatureFlag updatedFlag = featureFlagRepository.save(flag);
        flagChangePublisher.publish(FlagChangedEvent.from(
                updatedFlag,
                reason == null || reason.isBlank() ? "flag-toggled" : reason
        ));

        return updatedFlag;
    }
}
