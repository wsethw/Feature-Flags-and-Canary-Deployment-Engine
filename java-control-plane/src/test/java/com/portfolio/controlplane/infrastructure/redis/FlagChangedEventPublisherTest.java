package com.portfolio.controlplane.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.controlplane.domain.event.FlagChangedEvent;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagChangedEventPublisherTest {

    @Test
    void shouldPublishRedisSideEffectsOnlyAfterTransactionCommit() {
        RecordingRedisFlagChangeSink redisSink = new RecordingRedisFlagChangeSink();
        FlagChangedEventPublisher publisher = new FlagChangedEventPublisher(
                redisSink,
                new ObjectMapper().findAndRegisterModules(),
                "feature-flags:state:"
        );
        FeatureFlag flag = new FeatureFlag(
                UUID.randomUUID(),
                "new-checkout",
                "Routes checkout traffic",
                true,
                "production",
                List.of()
        );

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            publisher.publish(FlagChangedEvent.from(flag, "test"));

            assertTrue(redisSink.writes().isEmpty());

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            assertEquals(1, redisSink.writes().size());
            RedisWrite write = redisSink.writes().getFirst();
            assertEquals("feature-flags:state:new-checkout", write.stateKey());
            assertTrue(write.payload().contains("\"key\":\"new-checkout\""));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    private static final class RecordingRedisFlagChangeSink implements RedisFlagChangeSink {
        private final List<@NonNull RedisWrite> writes = new ArrayList<>();

        @Override
        public void storeAndPublish(@NonNull String stateKey, @NonNull String payload) {
            writes.add(new RedisWrite(stateKey, payload));
        }

        private @NonNull List<@NonNull RedisWrite> writes() {
            return writes;
        }
    }

    private record RedisWrite(@NonNull String stateKey, @NonNull String payload) {
    }
}
