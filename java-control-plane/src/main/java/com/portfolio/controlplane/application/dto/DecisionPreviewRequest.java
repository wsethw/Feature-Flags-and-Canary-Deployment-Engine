package com.portfolio.controlplane.application.dto;

import jakarta.validation.constraints.NotBlank;

public record DecisionPreviewRequest(
        @NotBlank String userId,
        @NotBlank String country,
        @NotBlank String platform
) {
}

