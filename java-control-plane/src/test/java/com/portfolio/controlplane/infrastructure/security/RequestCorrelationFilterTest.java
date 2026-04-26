package com.portfolio.controlplane.infrastructure.security;

import com.portfolio.controlplane.interfaces.rest.advice.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RequestCorrelationFilterTest {

    @Test
    void shouldExposeGeneratedRequestIdToErrorResponses() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/flags");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String generatedRequestId = (String) request.getAttribute(RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE);
        assertNotNull(generatedRequestId);
        assertEquals(generatedRequestId, response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER));

        ApiExceptionHandler handler = new ApiExceptionHandler();
        var errorResponse = handler.handleBadRequest(new IllegalArgumentException("bad request"), request);
        var body = errorResponse.getBody();
        if (body == null) {
            throw new AssertionError("Expected an API error body.");
        }

        assertEquals(generatedRequestId, body.requestId());
    }
}
