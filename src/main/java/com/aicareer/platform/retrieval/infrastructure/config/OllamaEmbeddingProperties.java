package com.aicareer.platform.retrieval.infrastructure.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.retrieval.embedding.ollama")
public record OllamaEmbeddingProperties(
        URI endpoint,
        String model,
        int dimensions,
        Duration timeout,
        int maxAttempts,
        Duration retryDelay
) {
}
