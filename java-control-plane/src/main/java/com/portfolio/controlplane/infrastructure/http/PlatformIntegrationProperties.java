package com.portfolio.controlplane.infrastructure.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integrations")
public class PlatformIntegrationProperties {

    private String dataPlaneBaseUrl = "http://localhost:8080";
    private String guardianBaseUrl = "http://localhost:8083";
    private String stableBaseUrl = "http://localhost:8084";
    private String canaryBaseUrl = "http://localhost:8085";

    public String getDataPlaneBaseUrl() {
        return dataPlaneBaseUrl;
    }

    public void setDataPlaneBaseUrl(String dataPlaneBaseUrl) {
        this.dataPlaneBaseUrl = dataPlaneBaseUrl;
    }

    public String getGuardianBaseUrl() {
        return guardianBaseUrl;
    }

    public void setGuardianBaseUrl(String guardianBaseUrl) {
        this.guardianBaseUrl = guardianBaseUrl;
    }

    public String getStableBaseUrl() {
        return stableBaseUrl;
    }

    public void setStableBaseUrl(String stableBaseUrl) {
        this.stableBaseUrl = stableBaseUrl;
    }

    public String getCanaryBaseUrl() {
        return canaryBaseUrl;
    }

    public void setCanaryBaseUrl(String canaryBaseUrl) {
        this.canaryBaseUrl = canaryBaseUrl;
    }
}

