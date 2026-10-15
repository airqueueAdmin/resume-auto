package com.aicareer.platform.ai.infrastructure.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ocr.gemini")
public record GeminiProperties(
        URI endpoint,
        String apiKey,
        String model,
        Duration timeout,
        int maxAttempts,
        Duration retryDelay,
        int maxOutputTokens
) {
}
