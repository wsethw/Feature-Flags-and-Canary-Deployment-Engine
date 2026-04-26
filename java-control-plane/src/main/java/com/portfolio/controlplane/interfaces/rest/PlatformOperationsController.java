package com.portfolio.controlplane.interfaces.rest;

import com.portfolio.controlplane.application.dto.DecisionPreviewRequest;
import com.portfolio.controlplane.application.dto.DecisionPreviewResponse;
import com.portfolio.controlplane.application.dto.PlatformOverviewResponse;
import com.portfolio.controlplane.application.service.PlatformOperationsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
public class PlatformOperationsController {

    private final PlatformOperationsService platformOperationsService;

    public PlatformOperationsController(PlatformOperationsService platformOperationsService) {
        this.platformOperationsService = platformOperationsService;
    }

    @GetMapping("/overview")
    public PlatformOverviewResponse getOverview() {
        return platformOperationsService.getOverview();
    }

    @PostMapping("/decision-preview")
    public DecisionPreviewResponse previewDecision(@Valid @RequestBody DecisionPreviewRequest request) {
        return platformOperationsService.previewDecision(request);
    }
}

