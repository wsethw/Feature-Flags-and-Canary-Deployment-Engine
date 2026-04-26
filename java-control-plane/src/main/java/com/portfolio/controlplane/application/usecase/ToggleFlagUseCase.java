package com.portfolio.controlplane.application.usecase;

import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.application.port.FlagChangePublisher;
import com.portfolio.controlplane.application.exception.FeatureFlagNotFoundException;
import com.portfolio.controlplane.domain.event.FlagChangedEvent;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
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
    public @NonNull FeatureFlag execute(@NonNull UUID flagId, boolean enabled, @Nullable String reason) {
        UUID safeFlagId = Objects.requireNonNull(flagId);
        FeatureFlag flag = featureFlagRepository.findById(safeFlagId)
                .orElseThrow(() -> new FeatureFlagNotFoundException(safeFlagId));

        flag.toggle(enabled);

        FeatureFlag updatedFlag = featureFlagRepository.save(flag);
        flagChangePublisher.publish(FlagChangedEvent.from(
                updatedFlag,
                reason == null || reason.isBlank() ? "flag-toggled" : reason
        ));

        return updatedFlag;
    }
}
