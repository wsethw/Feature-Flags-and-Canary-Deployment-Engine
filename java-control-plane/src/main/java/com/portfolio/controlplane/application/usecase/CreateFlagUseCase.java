package com.portfolio.controlplane.application.usecase;

import com.portfolio.controlplane.application.dto.CreateFlagRequest;
import com.portfolio.controlplane.application.exception.FeatureFlagAlreadyExistsException;
import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.application.port.FlagChangePublisher;
import com.portfolio.controlplane.domain.event.FlagChangedEvent;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import org.eclipse.jdt.annotation.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
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
    public @NonNull FeatureFlag execute(@NonNull CreateFlagRequest request) {
        CreateFlagRequest safeRequest = Objects.requireNonNull(request);
        featureFlagRepository.findByKey(safeRequest.key()).ifPresent(existing -> {
            throw new FeatureFlagAlreadyExistsException(safeRequest.key());
        });

        FeatureFlag flag = FeatureFlag.create(
                UUID.randomUUID(),
                safeRequest.key(),
                safeRequest.description(),
                safeRequest.enabled() != null && safeRequest.enabled(),
                safeRequest.environmentName()
        );

        FeatureFlag savedFlag;
        try {
            savedFlag = featureFlagRepository.save(flag);
        } catch (DataIntegrityViolationException exception) {
            throw new FeatureFlagAlreadyExistsException(flag.getKey(), exception);
        }

        flagChangePublisher.publish(FlagChangedEvent.from(savedFlag, "flag-created"));
        return savedFlag;
    }
}
