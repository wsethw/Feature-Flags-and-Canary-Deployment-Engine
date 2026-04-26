package com.portfolio.controlplane.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.controlplane.application.port.FlagChangePublisher;
import com.portfolio.controlplane.domain.event.FlagChangedEvent;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

@Component
public class FlagChangedEventPublisher implements FlagChangePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlagChangedEventPublisher.class);
    private static final int MAX_PUBLISH_ATTEMPTS = 3;

    private final @NonNull RedisFlagChangeSink redisSink;
    private final @NonNull ObjectMapper objectMapper;
    private final @NonNull String statePrefix;

    public FlagChangedEventPublisher(
            @NonNull RedisFlagChangeSink redisSink,
            @NonNull ObjectMapper objectMapper,
            @Value("${app.redis.state-prefix}") @NonNull String statePrefix
    ) {
        this.redisSink = Objects.requireNonNull(redisSink);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.statePrefix = requireText(statePrefix, "Redis state prefix must not be blank");
    }

    @Override
    public void publish(@NonNull FlagChangedEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAfterCommit(event);
                }
            });
            return;
        }

        publishWithRetry(event);
    }

    private void publishAfterCommit(@NonNull FlagChangedEvent event) {
        try {
            publishWithRetry(event);
        } catch (RuntimeException exception) {
            LOGGER.error("Unable to publish feature flag change after commit for key {}", event.key(), exception);
        }
    }

    private void publishWithRetry(@NonNull FlagChangedEvent event) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_PUBLISH_ATTEMPTS; attempt++) {
            try {
                publishNow(event);
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                LOGGER.warn(
                        "Feature flag publish attempt {} of {} failed for key {}",
                        attempt,
                        MAX_PUBLISH_ATTEMPTS,
                        event.key(),
                        exception
                );
                if (attempt < MAX_PUBLISH_ATTEMPTS) {
                    pauseBeforeRetry();
                }
            }
        }

        throw new IllegalStateException("Unable to publish feature flag change to Redis", lastFailure);
    }

    private void pauseBeforeRetry() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying feature flag publish", exception);
        }
    }

    private void publishNow(@NonNull FlagChangedEvent event) {
        @NonNull String payload = serialize(event);
        @NonNull String stateKey = statePrefix + requireFlagKey(event);

        redisSink.storeAndPublish(stateKey, payload);
    }

    private @NonNull String requireFlagKey(@NonNull FlagChangedEvent event) {
        String key = event.key();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Flag event key must not be blank.");
        }
        return key;
    }

    private @NonNull String serialize(@NonNull FlagChangedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize FlagChangedEvent", exception);
        }
    }

    private static @NonNull String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
