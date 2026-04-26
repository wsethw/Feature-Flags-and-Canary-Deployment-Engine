package com.portfolio.controlplane.application.usecase;

import com.portfolio.controlplane.application.dto.CreateFlagRequest;
import com.portfolio.controlplane.application.exception.FeatureFlagAlreadyExistsException;
import com.portfolio.controlplane.application.port.FeatureFlagRepository;
import com.portfolio.controlplane.application.port.FlagChangePublisher;
import com.portfolio.controlplane.domain.event.FlagChangedEvent;
import com.portfolio.controlplane.domain.model.FeatureFlag;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateFlagUseCaseTest {

    @Test
    void shouldConvertConcurrentUniqueKeyViolationToConflictException() {
        FeatureFlagRepository repository = new FeatureFlagRepository() {
            @Override
            public @NonNull FeatureFlag save(@NonNull FeatureFlag flag) {
                throw new DataIntegrityViolationException("duplicate flag_key");
            }

            @Override
            public @NonNull Optional<@NonNull FeatureFlag> findById(@NonNull UUID id) {
                return Optional.empty();
            }

            @Override
            public @NonNull Optional<@NonNull FeatureFlag> findByKey(@Nullable String key) {
                return Optional.empty();
            }

            @Override
            public @NonNull List<@NonNull FeatureFlag> findAll() {
                return List.of();
            }
        };
        FlagChangePublisher publisher = new FailingPublisher();
        CreateFlagUseCase useCase = new CreateFlagUseCase(repository, publisher);

        assertThrows(FeatureFlagAlreadyExistsException.class, () -> useCase.execute(new CreateFlagRequest(
                "new-checkout",
                "duplicate request racing another insert",
                false,
                "production"
        )));
    }

    private static final class FailingPublisher implements FlagChangePublisher {
        @Override
        public void publish(@NonNull FlagChangedEvent event) {
            throw new AssertionError("A duplicate insert must not publish an update.");
        }
    }
}
