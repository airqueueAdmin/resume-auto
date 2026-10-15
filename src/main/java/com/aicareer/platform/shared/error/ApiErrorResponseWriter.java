package com.aicareer.platform.shared.error;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ApiErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String detail
    ) throws IOException {
        String requestId = requestId(request);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(RequestIdFilter.REQUEST_ID_HEADER, requestId);

        ApiErrorResponse error = new ApiErrorResponse(
                "about:blank",
                status.getReasonPhrase(),
                status.value(),
                detail,
                request.getRequestURI(),
                Instant.now(),
                code,
                requestId,
                Map.of()
        );
        objectMapper.writeValue(response.getOutputStream(), error);
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        if (requestId != null) {
            return requestId.toString();
        }

        String headerValue = request.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        return headerValue == null || headerValue.isBlank() ? UUID.randomUUID().toString() : headerValue;
    }
}
