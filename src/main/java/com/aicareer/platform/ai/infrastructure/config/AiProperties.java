package com.aicareer.platform.ai.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        String provider,
        String promptVersion,
        Duration timeout,
        int maxAttempts,
        Duration retryDelay,
        int maxOutputTokens,
        double temperature
) {
}
