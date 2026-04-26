package com.portfolio.controlplane.application.usecase;

import com.portfolio.controlplane.application.dto.CreateFlagRequest;
import com.portfolio.controlplane.application.exception.FeatureFlagAlreadyExistsException;
import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.application.port.FlagChangePublisher;
import com.portfolio.controlplane.domain.event.FlagChangedEvent;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreateFlagUseCase {

    private final FeatureFlagRepository featureFlagRepository;
    private final FlagChangePublisher flagChangePublisher;

    public CreateFlagUseCase(
            FeatureFlagRepository featureFlagRepository,
            FlagChangePublisher flagChangePublisher
    ) {
        this.featureFlagRepository = featureFlagRepository;
        this.flagChangePublisher = flagChangePublisher;
    }

    @Transactional
    public FeatureFlag execute(CreateFlagRequest request) {
        featureFlagRepository.findByKey(request.key()).ifPresent(existing -> {
            throw new FeatureFlagAlreadyExistsException(request.key());
        });

        FeatureFlag flag = FeatureFlag.create(
                UUID.randomUUID(),
                request.key(),
                request.description(),
                request.enabled() != null && request.enabled(),
                request.environmentName()
        );

        FeatureFlag savedFlag = featureFlagRepository.save(flag);
        flagChangePublisher.publish(FlagChangedEvent.from(savedFlag, "flag-created"));
        return savedFlag;
    }
}
