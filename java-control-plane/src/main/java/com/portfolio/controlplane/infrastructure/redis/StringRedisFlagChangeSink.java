package com.portfolio.controlplane.infrastructure.redis;

import org.eclipse.jdt.annotation.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
class StringRedisFlagChangeSink implements RedisFlagChangeSink {

    private final @NonNull StringRedisTemplate redisTemplate;
    private final @NonNull String updateChannel;
    private final @NonNull String stateIndexKey;

    StringRedisFlagChangeSink(
            @NonNull StringRedisTemplate redisTemplate,
            @Value("${app.redis.update-channel}") @NonNull String updateChannel,
            @Value("${app.redis.state-index-key}") @NonNull String stateIndexKey
    ) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
        this.updateChannel = requireText(updateChannel, "Redis update channel must not be blank");
        this.stateIndexKey = requireText(stateIndexKey, "Redis state index key must not be blank");
    }

    @Override
    public void storeAndPublish(@NonNull String stateKey, @NonNull String payload) {
        redisTemplate.opsForValue().set(stateKey, payload);
        redisTemplate.opsForSet().add(stateIndexKey, stateKey);
        redisTemplate.convertAndSend(updateChannel, payload);
    }

    private static @NonNull String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
