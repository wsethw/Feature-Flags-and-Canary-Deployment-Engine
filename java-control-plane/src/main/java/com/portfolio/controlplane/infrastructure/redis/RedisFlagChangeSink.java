package com.portfolio.controlplane.infrastructure.redis;

import org.eclipse.jdt.annotation.NonNull;

interface RedisFlagChangeSink {

    void storeAndPublish(@NonNull String stateKey, @NonNull String payload);
}
