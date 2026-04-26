package com.portfolio.controlplane.interfaces.rest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DashboardRenderingSecurityTest {

    @Test
    void dashboardScriptMustNotRenderApiDataThroughHtmlSinks() throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream("static/assets/app.js")) {
            assertNotNull(stream);
            String script = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertFalse(script.contains(".innerHTML"));
            assertFalse(script.contains("insertAdjacentHTML"));
        }
    }
}
