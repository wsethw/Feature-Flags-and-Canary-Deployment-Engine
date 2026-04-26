package com.portfolio.controlplane.interfaces.rest;

import com.portfolio.controlplane.application.dto.FeatureFlagResponse;
import com.portfolio.controlplane.application.query.GetFeatureFlagQuery;
import com.portfolio.controlplane.application.query.ListFeatureFlagsQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/flags")
public class FlagQueryController {

    private final ListFeatureFlagsQuery listFeatureFlagsQuery;
    private final GetFeatureFlagQuery getFeatureFlagQuery;

    public FlagQueryController(
            ListFeatureFlagsQuery listFeatureFlagsQuery,
            GetFeatureFlagQuery getFeatureFlagQuery
    ) {
        this.listFeatureFlagsQuery = listFeatureFlagsQuery;
        this.getFeatureFlagQuery = getFeatureFlagQuery;
    }

    @GetMapping
    public List<FeatureFlagResponse> listFlags() {
        return listFeatureFlagsQuery.execute().stream()
                .map(FeatureFlagResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public FeatureFlagResponse getFlag(@PathVariable("id") UUID flagId) {
        return FeatureFlagResponse.from(getFeatureFlagQuery.byId(flagId));
    }
}

