package com.portfolio.controlplane.infrastructure.config;

import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.application.port.FlagChangePublisher;
import com.portfolio.controlplane.domain.event.FlagChangedEvent;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import com.portfolio.controlplane.domain.model.RuleOperator;
import com.portfolio.controlplane.domain.model.TargetVersion;
import com.portfolio.controlplane.domain.model.TargetingRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class SeedDataBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeedDataBootstrap.class);

    private final FeatureFlagRepository featureFlagRepository;
    private final FlagChangePublisher flagChangePublisher;
    private final UUID defaultFlagId;
    private final String defaultEnvironment;
    private final boolean defaultFlagEnabled;

    public SeedDataBootstrap(
            FeatureFlagRepository featureFlagRepository,
            FlagChangePublisher flagChangePublisher,
            @Value("${app.seed.default-flag-id}") UUID defaultFlagId,
            @Value("${app.seed.default-environment}") String defaultEnvironment,
            @Value("${app.seed.default-flag-enabled}") boolean defaultFlagEnabled
    ) {
        this.featureFlagRepository = featureFlagRepository;
        this.flagChangePublisher = flagChangePublisher;
        this.defaultFlagId = defaultFlagId;
        this.defaultEnvironment = defaultEnvironment;
        this.defaultFlagEnabled = defaultFlagEnabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        featureFlagRepository.findByKey("new-checkout").ifPresentOrElse(
                existingFlag -> LOGGER.info("Seed flag already present with id {}", existingFlag.getId()),
                this::seedDefaultFlag
        );

        featureFlagRepository.findAll().forEach(flag ->
                flagChangePublisher.publish(FlagChangedEvent.from(flag, "startup-sync"))
        );
    }

    private void seedDefaultFlag() {
        FeatureFlag defaultFlag = FeatureFlag.create(
                defaultFlagId,
                "new-checkout",
                "Routes a controlled portion of BR iOS users to the canary checkout flow",
                defaultFlagEnabled,
                defaultEnvironment
        );

        defaultFlag.addRule(new TargetingRule("country", RuleOperator.EQUALS, "BR", TargetVersion.CANARY));
        defaultFlag.addRule(new TargetingRule("platform", RuleOperator.EQUALS, "iOS", TargetVersion.CANARY));
        defaultFlag.addRule(new TargetingRule("userId", RuleOperator.PERCENTAGE, "10", TargetVersion.CANARY));

        FeatureFlag saved = featureFlagRepository.save(defaultFlag);
        LOGGER.info("Seeded default feature flag {} for environment {}", saved.getKey(), saved.getEnvironmentName());
    }
}
