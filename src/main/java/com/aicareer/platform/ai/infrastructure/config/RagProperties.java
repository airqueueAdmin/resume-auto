package com.aicareer.platform.ai.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.matching")
public record RagProperties(
        String promptVersion,
        int topK,
        int maxContextCharacters
) {
}
