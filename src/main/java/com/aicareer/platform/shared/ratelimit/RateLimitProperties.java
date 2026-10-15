package com.aicareer.platform.shared.ratelimit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Duration window,
        int maxRequests
) {
    public RateLimitProperties {
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate-limit window must be positive");
        }
        if (maxRequests < 1) {
            throw new IllegalArgumentException("Rate-limit maxRequests must be positive");
        }
    }
}
