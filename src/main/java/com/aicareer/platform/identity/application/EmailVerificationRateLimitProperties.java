package com.aicareer.platform.identity.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.identity.email-verification.rate-limit")
public record EmailVerificationRateLimitProperties(
        boolean enabled,
        Duration clientWindow,
        int maxClientRequests,
        Duration deliveryWindow,
        int maxDeliveries
) {
    public EmailVerificationRateLimitProperties {
        requirePositive(clientWindow, "Email verification client rate-limit window");
        requirePositive(deliveryWindow, "Email verification delivery rate-limit window");
        if (maxClientRequests < 1) {
            throw new IllegalArgumentException("Email verification max client requests must be positive");
        }
        if (maxDeliveries < 1) {
            throw new IllegalArgumentException("Email verification max deliveries must be positive");
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
