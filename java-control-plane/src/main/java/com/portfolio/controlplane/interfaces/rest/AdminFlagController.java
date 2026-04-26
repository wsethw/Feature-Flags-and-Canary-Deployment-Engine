package com.portfolio.controlplane.interfaces.rest;

import com.portfolio.controlplane.application.dto.AddTargetingRuleRequest;
import com.portfolio.controlplane.application.dto.CreateFlagRequest;
import com.portfolio.controlplane.application.dto.FeatureFlagResponse;
import com.portfolio.controlplane.application.dto.ToggleFlagRequest;
import com.portfolio.controlplane.application.usecase.AddTargetingRuleUseCase;
import com.portfolio.controlplane.application.usecase.CreateFlagUseCase;
import com.portfolio.controlplane.application.usecase.ToggleFlagUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/flags")
public class AdminFlagController {

    private final CreateFlagUseCase createFlagUseCase;
    private final ToggleFlagUseCase toggleFlagUseCase;
    private final AddTargetingRuleUseCase addTargetingRuleUseCase;

    public AdminFlagController(
            CreateFlagUseCase createFlagUseCase,
            ToggleFlagUseCase toggleFlagUseCase,
            AddTargetingRuleUseCase addTargetingRuleUseCase
    ) {
        this.createFlagUseCase = createFlagUseCase;
        this.toggleFlagUseCase = toggleFlagUseCase;
        this.addTargetingRuleUseCase = addTargetingRuleUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeatureFlagResponse createFlag(@Valid @RequestBody CreateFlagRequest request) {
        return FeatureFlagResponse.from(createFlagUseCase.execute(request));
    }

    @PutMapping("/{id}/toggle")
    public FeatureFlagResponse toggleFlag(
            @PathVariable("id") UUID flagId,
            @Valid @RequestBody ToggleFlagRequest request
    ) {
        return FeatureFlagResponse.from(toggleFlagUseCase.execute(flagId, request.enabled(), request.reason()));
    }

    @PostMapping("/{id}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public FeatureFlagResponse addRule(
            @PathVariable("id") UUID flagId,
            @Valid @RequestBody AddTargetingRuleRequest request
    ) {
        return FeatureFlagResponse.from(addTargetingRuleUseCase.execute(flagId, request));
    }
}
