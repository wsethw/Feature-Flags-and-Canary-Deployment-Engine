package com.portfolio.controlplane.infrastructure.persistence;

import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.domain.model.FeatureFlag;
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
    public FeatureFlag save(FeatureFlag flag) {
        FeatureFlagEntity entity = featureFlagMapper.toEntity(flag);
        return featureFlagMapper.toDomain(featureFlagJpaRepository.save(entity));
    }

    @Override
    public Optional<FeatureFlag> findById(UUID id) {
        return featureFlagJpaRepository.findById(id).map(featureFlagMapper::toDomain);
    }

    @Override
    public Optional<FeatureFlag> findByKey(String key) {
        return featureFlagJpaRepository.findByFlagKey(key).map(featureFlagMapper::toDomain);
    }

    @Override
    public List<FeatureFlag> findAll() {
        return featureFlagJpaRepository.findAll().stream()
                .map(featureFlagMapper::toDomain)
                .toList();
    }
}

