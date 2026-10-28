package com.aicareer.platform.retrieval.infrastructure.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.retrieval.embedding.gemini")
public record GeminiEmbeddingProperties(
        URI endpoint,
        String apiKey,
        String model,
        int dimensions,
        Duration timeout,
        int maxAttempts,
        Duration retryDelay
) {
}
