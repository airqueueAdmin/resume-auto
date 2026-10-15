package com.aicareer.platform.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.aicareer.platform.shared.ratelimit.RateLimitExceededException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void illegalArgumentIsConvertedToBadRequestResponse() {
        MockHttpServletRequest request = request("/api/v1/example");
        request.setAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE, "request-123");

        ResponseEntity<ApiErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("invalid example"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_ARGUMENT");
        assertThat(response.getBody().requestId()).isEqualTo("request-123");
        assertThat(response.getBody().fieldErrors()).isEqualTo(Map.of());
    }

    @Test
    void rateLimitIsConvertedToTooManyRequestsWithRetryAfter() {
        MockHttpServletRequest request = request("/api/v1/ai/match-job");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ResponseEntity<ApiErrorResponse> response = handler.handleRateLimit(
                new RateLimitExceededException(12),
                request,
                servletResponse
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RATE_LIMIT_EXCEEDED");
        assertThat(servletResponse.getHeader("Retry-After")).isEqualTo("12");
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
