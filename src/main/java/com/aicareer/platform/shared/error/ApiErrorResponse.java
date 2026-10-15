package com.aicareer.platform.shared.error;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        Instant timestamp,
        String code,
        String requestId,
        Map<String, String> fieldErrors
) {
    public ApiErrorResponse {
        fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
    }
}
