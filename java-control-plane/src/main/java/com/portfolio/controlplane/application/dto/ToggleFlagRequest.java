package com.portfolio.controlplane.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public record ToggleFlagRequest(
        @NotNull @NonNull Boolean enabled,
        @Size(max = 200, message = "Reason must have at most 200 characters.")
        @Nullable String reason
) {
}
