package com.aicareer.platform.retrieval.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.retrieval.embedding")
public record EmbeddingProviderProperties(String provider) {
}
