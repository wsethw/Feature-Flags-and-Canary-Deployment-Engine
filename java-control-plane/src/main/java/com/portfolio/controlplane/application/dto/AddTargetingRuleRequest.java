package com.portfolio.controlplane.application.dto;

import com.portfolio.controlplane.domain.model.RuleOperator;
import com.portfolio.controlplane.domain.model.TargetVersion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.eclipse.jdt.annotation.NonNull;

public record AddTargetingRuleRequest(
        @NotBlank
        @Pattern(regexp = "^(country|platform|userId)$", message = "Supported targeting attributes are country, platform and userId.")
        @NonNull String attribute,
        @NotNull @NonNull RuleOperator operator,
        @NotBlank
        @Size(max = 120, message = "Rule value must have at most 120 characters.")
        @NonNull String value,
        @NotNull @NonNull TargetVersion targetVersion
) {
}
