package com.portfolio.controlplane.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String REQUEST_ID_ATTRIBUTE = RequestCorrelationFilter.class.getName() + ".requestId";

    public static String getRequestId(HttpServletRequest request) {
        HttpServletRequest safeRequest = Objects.requireNonNull(request);
        Object requestId = safeRequest.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (requestId instanceof String value && !value.isBlank()) {
            return value;
        }
        return Optional.ofNullable(safeRequest.getHeader(REQUEST_ID_HEADER))
                .filter(value -> !value.isBlank())
                .orElse("unknown");
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

        String requestId = Optional.ofNullable(safeRequest.getHeader(REQUEST_ID_HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        safeRequest.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        safeResponse.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            safeFilterChain.doFilter(safeRequest, safeResponse);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
