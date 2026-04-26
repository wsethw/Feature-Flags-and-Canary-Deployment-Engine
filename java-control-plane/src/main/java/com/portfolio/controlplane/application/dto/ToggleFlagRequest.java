package com.portfolio.controlplane.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ToggleFlagRequest(
        @NotNull Boolean enabled,
        @Size(max = 200, message = "Reason must have at most 200 characters.")
        String reason
) {
}
