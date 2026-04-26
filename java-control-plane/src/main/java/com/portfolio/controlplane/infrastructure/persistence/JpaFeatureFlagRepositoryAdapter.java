package com.portfolio.controlplane.infrastructure.persistence;

import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaFeatureFlagRepositoryAdapter implements FeatureFlagRepository {

    private final FeatureFlagJpaRepository featureFlagJpaRepository;
    private final FeatureFlagMapper featureFlagMapper;

    public JpaFeatureFlagRepositoryAdapter(
            FeatureFlagJpaRepository featureFlagJpaRepository,
            FeatureFlagMapper featureFlagMapper
    ) {
        this.featureFlagJpaRepository = featureFlagJpaRepository;
        this.featureFlagMapper = featureFlagMapper;
    }

    @Override
    public @NonNull FeatureFlag save(@NonNull FeatureFlag flag) {
        FeatureFlagEntity entity = requireNonNull(featureFlagMapper.toEntity(flag), "Mapped entity must not be null");
        FeatureFlagEntity savedEntity = requireNonNull(
                featureFlagJpaRepository.saveAndFlush(entity),
                "Saved feature flag entity must not be null"
        );
        return featureFlagMapper.toDomain(savedEntity);
    }

    @Override
    public @NonNull Optional<@NonNull FeatureFlag> findById(@NonNull UUID id) {
        UUID safeId = requireNonNull(id, "Feature flag id must not be null");
        Optional<FeatureFlagEntity> entity = requireNonNull(
                featureFlagJpaRepository.findById(safeId),
                "Spring Data findById must not return null"
        );
        return entity.map(item -> featureFlagMapper.toDomain(
                requireNonNull(item, "Feature flag entity must not be null")
        ));
    }

    @Override
    public @NonNull Optional<@NonNull FeatureFlag> findByKey(@Nullable String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        Optional<FeatureFlagEntity> entity = requireNonNull(
                featureFlagJpaRepository.findByFlagKey(key.trim().toLowerCase()),
                "Spring Data findByFlagKey must not return null"
        );
        return entity.map(item -> featureFlagMapper.toDomain(
                requireNonNull(item, "Feature flag entity must not be null")
        ));
    }

    @Override
    public @NonNull List<@NonNull FeatureFlag> findAll() {
        List<FeatureFlagEntity> entities = requireNonNull(
                featureFlagJpaRepository.findAll(),
                "Spring Data findAll must not return null"
        );
        return entities.stream()
                .map(entity -> featureFlagMapper.toDomain(
                        requireNonNull(entity, "Feature flag entity must not be null")
                ))
                .toList();
    }

    private static <T> @NonNull T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
