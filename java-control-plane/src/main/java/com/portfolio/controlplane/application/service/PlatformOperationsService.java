package com.portfolio.controlplane.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.controlplane.application.dto.DecisionPreviewRequest;
import com.portfolio.controlplane.application.dto.DecisionPreviewResponse;
import com.portfolio.controlplane.application.dto.FeatureFlagResponse;
import com.portfolio.controlplane.application.dto.PlatformOverviewResponse;
import com.portfolio.controlplane.application.exception.ExternalDependencyException;
import com.portfolio.controlplane.application.query.ListFeatureFlagsQuery;
import com.portfolio.controlplane.infrastructure.http.PlatformIntegrationProperties;
import com.portfolio.controlplane.infrastructure.security.AdminSecurityProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class PlatformOperationsService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ListFeatureFlagsQuery listFeatureFlagsQuery;
    private final PlatformIntegrationProperties integrationProperties;
    private final AdminSecurityProperties adminSecurityProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PlatformOperationsService(
            ListFeatureFlagsQuery listFeatureFlagsQuery,
            PlatformIntegrationProperties integrationProperties,
            AdminSecurityProperties adminSecurityProperties,
            ObjectMapper objectMapper
    ) {
        this.listFeatureFlagsQuery = listFeatureFlagsQuery;
        this.integrationProperties = integrationProperties;
        this.adminSecurityProperties = adminSecurityProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public PlatformOverviewResponse getOverview() {
        List<FeatureFlagResponse> flags = listFeatureFlagsQuery.execute().stream()
                .map(FeatureFlagResponse::from)
                .toList();

        Object guardianTelemetry = safeFetchBody(
                integrationProperties.getGuardianBaseUrl(),
                "/api/telemetry/status",
                false
        );

        return new PlatformOverviewResponse(
                Instant.now(),
                flags,
                new PlatformOverviewResponse.ServiceStatus(
                        "control-plane",
                        "UP",
                        "local",
                        true,
                        Map.of(
                                "flags", flags.size(),
                                "environments", flags.stream().map(FeatureFlagResponse::environmentName).collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new)),
                                "adminTokenEnabled", StringUtils.hasText(adminSecurityProperties.getApiToken())
                        )
                ),
                fetchStatus("data-plane", integrationProperties.getDataPlaneBaseUrl(), "/health", false),
                fetchStatus("guardian", integrationProperties.getGuardianBaseUrl(), "/health", false),
                fetchStatus("stable", integrationProperties.getStableBaseUrl(), "/health", false),
                fetchStatus("canary", integrationProperties.getCanaryBaseUrl(), "/health", false),
                guardianTelemetry
        );
    }

    public DecisionPreviewResponse previewDecision(DecisionPreviewRequest request) {
        Map<String, Object> payload = fetchBody(
                integrationProperties.getDataPlaneBaseUrl(),
                "/internal/decision-preview",
                true,
                Map.of(
                        "flagKey", "new-checkout",
                        "context", Map.of(
                                "userId", request.userId(),
                                "country", request.country(),
                                "platform", request.platform()
                        )
                )
        );

        return objectMapper.convertValue(payload, DecisionPreviewResponse.class);
    }

    private PlatformOverviewResponse.ServiceStatus fetchStatus(
            String name,
            String baseUrl,
            String path,
            boolean secured
    ) {
        try {
            Map<String, Object> body = fetchBody(baseUrl, path, secured);
            Object status = body.getOrDefault("status", "UNKNOWN");
            return new PlatformOverviewResponse.ServiceStatus(
                    name,
                    String.valueOf(status),
                    baseUrl,
                    true,
                    body
            );
        } catch (Exception exception) {
            return new PlatformOverviewResponse.ServiceStatus(
                    name,
                    "UNREACHABLE",
                    baseUrl,
                    false,
                    Map.of("error", exception.getMessage())
            );
        }
    }

    private Map<String, Object> fetchBody(String baseUrl, String path, boolean secured) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(3))
                .GET();

        if (secured && StringUtils.hasText(adminSecurityProperties.getApiToken())) {
            builder.header("X-Admin-Token", adminSecurityProperties.getApiToken());
        }

        return send(builder.build());
    }

    private Map<String, Object> fetchBody(String baseUrl, String path, boolean secured, Map<String, Object> body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(body)));

        if (secured && StringUtils.hasText(adminSecurityProperties.getApiToken())) {
            builder.header("X-Admin-Token", adminSecurityProperties.getApiToken());
        }

        return send(builder.build());
    }

    private Map<String, Object> send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ExternalDependencyException("External service returned status " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (IOException exception) {
            throw new ExternalDependencyException("Unable to parse response from external service.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalDependencyException("External service request was interrupted.", exception);
        }
    }

    private String writeJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize request payload.", exception);
        }
    }

    private Map<String, Object> safeFetchBody(String baseUrl, String path, boolean secured) {
        try {
            return fetchBody(baseUrl, path, secured);
        } catch (Exception exception) {
            return Map.of(
                    "status", "UNREACHABLE",
                    "error", exception.getMessage(),
                    "baseUrl", baseUrl
            );
        }
    }
}
