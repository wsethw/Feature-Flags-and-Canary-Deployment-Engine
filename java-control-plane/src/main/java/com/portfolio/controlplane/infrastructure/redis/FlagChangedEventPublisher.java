package com.portfolio.controlplane.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.controlplane.application.port.FlagChangePublisher;
import com.portfolio.controlplane.domain.event.FlagChangedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class FlagChangedEventPublisher implements FlagChangePublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String updateChannel;
    private final String stateIndexKey;
    private final String statePrefix;

    public FlagChangedEventPublisher(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.redis.update-channel}") String updateChannel,
            @Value("${app.redis.state-index-key}") String stateIndexKey,
            @Value("${app.redis.state-prefix}") String statePrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.updateChannel = updateChannel;
        this.stateIndexKey = stateIndexKey;
        this.statePrefix = statePrefix;
    }

    @Override
    public void publish(FlagChangedEvent event) {
        String payload = serialize(event);
        String stateKey = statePrefix + event.key();

        redisTemplate.opsForValue().set(stateKey, payload);
        redisTemplate.opsForSet().add(stateIndexKey, stateKey);
        redisTemplate.convertAndSend(updateChannel, payload);
    }

    private String serialize(FlagChangedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize FlagChangedEvent", exception);
        }
    }
}

