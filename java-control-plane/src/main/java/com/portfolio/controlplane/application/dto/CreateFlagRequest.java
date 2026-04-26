package com.portfolio.controlplane.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFlagRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Flag key must use lowercase letters, digits and hyphens only.")
        @Size(max = 80, message = "Flag key must have at most 80 characters.")
        String key,
        @NotBlank
        @Size(max = 280, message = "Description must have at most 280 characters.")
        String description,
        Boolean enabled,
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Environment name must use lowercase letters, digits and hyphens only.")
        @Size(max = 40, message = "Environment name must have at most 40 characters.")
        String environmentName
) {
}
