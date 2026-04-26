package com.portfolio.controlplane.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AdminApiTokenFilter extends OncePerRequestFilter {

    private final AdminSecurityProperties adminSecurityProperties;
    private final ObjectMapper objectMapper;

    public AdminApiTokenFilter(
            AdminSecurityProperties adminSecurityProperties,
            ObjectMapper objectMapper
    ) {
        this.adminSecurityProperties = Objects.requireNonNull(adminSecurityProperties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        HttpServletRequest safeRequest = Objects.requireNonNull(request);
        String path = safeRequest.getRequestURI();
        return !(path.startsWith("/api/admin/") || path.startsWith("/api/platform/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        HttpServletRequest safeRequest = Objects.requireNonNull(request);
        HttpServletResponse safeResponse = Objects.requireNonNull(response);
        FilterChain safeFilterChain = Objects.requireNonNull(filterChain);

        String configuredToken = adminSecurityProperties.getApiToken();
        if (!StringUtils.hasText(configuredToken)) {
            safeFilterChain.doFilter(safeRequest, safeResponse);
            return;
        }

        if (tokensMatch(configuredToken, safeRequest.getHeader("X-Admin-Token"))) {
            safeFilterChain.doFilter(safeRequest, safeResponse);
            return;
        }

        safeResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        safeResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(safeResponse.getWriter(), Map.of(
                "timestamp", Instant.now(),
                "status", HttpServletResponse.SC_UNAUTHORIZED,
                "error", "Unauthorized",
                "message", "A valid admin token is required to access this resource.",
                "requestId", safeResponse.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)
        ));
    }

    private boolean tokensMatch(String configuredToken, @Nullable String presentedToken) {
        if (!StringUtils.hasText(presentedToken)) {
            return false;
        }

        byte[] configuredBytes = configuredToken.getBytes(StandardCharsets.UTF_8);
        byte[] presentedBytes = presentedToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(configuredBytes, presentedBytes);
    }
}
