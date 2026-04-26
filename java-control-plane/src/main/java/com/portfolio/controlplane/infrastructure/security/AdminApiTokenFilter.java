package com.portfolio.controlplane.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AdminApiTokenFilter extends OncePerRequestFilter {

    private final AdminSecurityProperties adminSecurityProperties;
    private final ObjectMapper objectMapper;

    public AdminApiTokenFilter(AdminSecurityProperties adminSecurityProperties, ObjectMapper objectMapper) {
        this.adminSecurityProperties = adminSecurityProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/api/admin/") || path.startsWith("/api/platform/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String configuredToken = adminSecurityProperties.getApiToken();
        if (!StringUtils.hasText(configuredToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        String presentedToken = request.getHeader("X-Admin-Token");
        if (configuredToken.equals(presentedToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "timestamp", Instant.now(),
                "status", HttpServletResponse.SC_UNAUTHORIZED,
                "error", "Unauthorized",
                "message", "A valid admin token is required to access this resource.",
                "requestId", response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)
        ));
    }
}

