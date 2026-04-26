package com.portfolio.controlplane.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.jdt.annotation.NonNull;

public record DecisionPreviewRequest(
        @NotBlank
        @Size(max = 120, message = "User ID must have at most 120 characters.")
        @NonNull String userId,
        @NotBlank
        @Size(max = 56, message = "Country must have at most 56 characters.")
        @NonNull String country,
        @NotBlank
        @Size(max = 56, message = "Platform must have at most 56 characters.")
        @NonNull String platform
) {
}
